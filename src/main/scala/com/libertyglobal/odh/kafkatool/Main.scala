/*
 *    Copyright 2017 Ilya Epifanov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 */

package com.libertyglobal.odh.kafkatool

import com.libertyglobal.odh.kafkatool.config.KafkaToolConfig
import com.libertyglobal.odh.kafkatool.partitionreassignment.{CleanupOp, PartitionReassignmentOp, PartitionsPerBroker, RepairOp}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.auto._
import io.circe.syntax._
import net.ceedubs.ficus.Ficus._
import org.apache.kafka.clients.admin.{AdminClient, DescribeClusterOptions, ListTopicsOptions}
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator

import scala.collection.JavaConverters._
import scala.collection.Map
import scala.reflect.io.File

object Main extends StrictLogging {

  def sortedToString[T: Ordering](xs: Iterable[T]): String = {
    xs.toSeq.sorted.mkString(",")
  }

  def overallPartitionDistributionPerBroker(config: KafkaToolConfig, currentReplications: Iterable[TopicReplicationInfo]): PartitionsPerBroker = {
    partitionDistributionPerBroker(config, currentReplications.flatMap(_.partitions.values))
  }

  def partitionDistributionPerBroker(config: KafkaToolConfig, currentReplications: Iterable[PartitionReplicationInfo]): PartitionsPerBroker = {
    val ret = currentReplications
      .flatMap(_.effectiveReplicas)
      .groupBy(identity)
      .filterKeys(config.brokerIds.contains)
      .mapValues(_.size)

    val missingKeys = config.brokerIds -- ret.keySet

    PartitionsPerBroker(ret ++ missingKeys.map(_ -> 0))
  }

  def processTopics(config: KafkaToolConfig,
                    currentReplications: Map[String, TopicReplicationInfo],
                    op: PartitionReassignmentOp
                   ): Map[String, TargetTopicReplicationInfo] = {
    var allPartitionsPerBroker = overallPartitionDistributionPerBroker(config, currentReplications.values)

    logger.info("Distribution of partitions across brokers before suggested repair")
    for ((broker, partitions) <- allPartitionsPerBroker.distribution.toSeq.sorted) {
      logger.info(f"Broker $broker%2d: $partitions%4d")
    }

    def processTopic(partitions: Map[PartitionId, PartitionReplicationInfo], rf: Int): Map[PartitionId, TargetPartitionReplicationInfo] = {
      var partitionsPerBroker = partitionDistributionPerBroker(config, partitions.values)

      def processPartition(currentReplicationInfo: PartitionReplicationInfo): TargetPartitionReplicationInfo = {
        var currentReplication = TargetPartitionReplicationInfo(currentReplicationInfo.effectiveReplicas, currentReplicationInfo.effectiveReplicas)

        for (_ <- currentReplication.targetReplicas.size.until(rf)) {
          op.addBroker(currentReplication, partitionsPerBroker, allPartitionsPerBroker).foreach { bestBroker =>
            partitionsPerBroker = partitionsPerBroker.inc(bestBroker)
            allPartitionsPerBroker = allPartitionsPerBroker.inc(bestBroker)
            currentReplication += bestBroker
          }
        }

        for (_ <- rf.until(currentReplication.targetReplicas.size)) {
          op.removeBroker(currentReplication, partitionsPerBroker, allPartitionsPerBroker).foreach { bestBroker =>
            partitionsPerBroker = partitionsPerBroker.dec(bestBroker)
            allPartitionsPerBroker = allPartitionsPerBroker.dec(bestBroker)
            currentReplication -= bestBroker
          }
        }

        currentReplication
      }

      for {
        (partition, partitionInfo) <- partitions if partitionInfo.effectiveReplicas.size != rf && partitionInfo.effectiveReplicas.nonEmpty
        reassignmentPlan = processPartition(partitionInfo) if !reassignmentPlan.isTrivial
      } yield {
        partition -> reassignmentPlan
      }
    }

    val ret = for {
      (topic, currentReplication) <- currentReplications
      rf <- config.topicSettings(topic).rf
      repairedTopic = processTopic(currentReplication.partitions, rf) if repairedTopic.nonEmpty
    } yield {
      topic -> TargetTopicReplicationInfo(repairedTopic)
    }

    logger.info("Distribution of partitions across brokers after suggested repair")
    for ((broker, partitions) <- allPartitionsPerBroker.distribution.toSeq.sorted) {
      logger.info(f"Broker $broker%2d: $partitions%4d")
    }

    ret
  }

  def main(args: Array[String]): Unit = {
    val opts = new Opts(args)

    if (opts.verbose.getOrElse(false)) {
      Configurator.setLevel("com.libertyglobal", Level.DEBUG)
    }

    val op = opts.subcommand match {
      case Some(c) if c == opts.cleanup => new CleanupOp()
      case Some(c) if c == opts.repair => new RepairOp()
      case _ =>
        opts.printHelp()
        sys.exit(1)
    }
    val config = ConfigFactory.load().as[KafkaToolConfig]("kafka-tool")

    val kafka = AdminClient.create(config.kafka.asJava)

    lazy val describeClusterResult = kafka.describeCluster(new DescribeClusterOptions())
    lazy val clusterId = describeClusterResult.clusterId().get()
    lazy val controllerNode = describeClusterResult.controller().get()
    lazy val nodes = describeClusterResult.nodes().get().asScala.toSeq

    logger.debug(s"Cluster id: $clusterId")
    logger.debug(s"Controller node: ${controllerNode.idString()}")
    logger.debug(s"Nodes: ${sortedToString(nodes.map(_.id()))}")

    val topics = kafka.listTopics(new ListTopicsOptions().listInternal(true)).listings().get().asScala

    val topicDescriptions = kafka.describeTopics(topics.map(_.name()).asJavaCollection).all().get().asScala.toMap

    val currentReplication = topicDescriptions.mapValues { td =>
      val partitions: Map[Int, PartitionReplicationInfo] = td.partitions().asScala.map { partitionInfo =>
        val replicas = partitionInfo.replicas().asScala.map(_.id()).toSet
        partitionInfo.partition() -> PartitionReplicationInfo(replicas, replicas.intersect(config.brokerIds))
      }(collection.breakOut)
      TopicReplicationInfo(partitions)
    }

    val targetReplication = processTopics(config, currentReplication, op)

    if (logger.underlying.isDebugEnabled) {
      for ((topic, partitions) <- targetReplication) {
        logger.debug(s"Required changes for topic $topic")

        for ((partition, changes) <- partitions.partitions) {
          logger.debug(s"Partition $partition: ${sortedToString(changes.targetReplicas)} <- ${sortedToString(changes.effectiveReplicas)}")
        }
      }
    }

    val reassignmentJson = formatAsReassignmentJson(targetReplication)

    opts.out.toOption match {
      case Some(outputFilename) =>
        File(outputFilename).writeAll(reassignmentJson)
        logger.info(s"Written reassignment plan to `$outputFilename`")
      case None =>
        logger.info("Here goes the reassignment JSON:")
        logger.info(reassignmentJson)
    }
  }

  def formatAsReassignmentJson(topicsToBeRepaired: Map[String, TargetTopicReplicationInfo]): String = {
    Reassignment(partitions = topicsToBeRepaired.flatMap({ case (topic, partitions) =>
      for (p <- partitions.partitions) yield {
        PartitionReassignment(topic, p._1, p._2.targetReplicas.toSeq)
      }
    }).toSeq).asJson.spaces2
  }
}

case class TopicReplicationInfo(partitions: Map[PartitionId, PartitionReplicationInfo])

case class TargetTopicReplicationInfo(partitions: Map[PartitionId, TargetPartitionReplicationInfo])

case class PartitionReplicationInfo(replicas: Set[BrokerId], effectiveReplicas: Set[BrokerId])

case class TargetPartitionReplicationInfo(effectiveReplicas: Set[BrokerId], targetReplicas: Set[BrokerId]) {
  def +(broker: BrokerId): TargetPartitionReplicationInfo = {
    TargetPartitionReplicationInfo(effectiveReplicas, targetReplicas + broker)
  }

  def -(broker: BrokerId): TargetPartitionReplicationInfo = {
    TargetPartitionReplicationInfo(effectiveReplicas, targetReplicas - broker)
  }

  def isTrivial: Boolean = effectiveReplicas == targetReplicas
}
