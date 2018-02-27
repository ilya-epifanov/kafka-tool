/*
 *    Copyright 2018 Ilya Epifanov
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

package com.libertyglobal.odh.kafkatool.config

import com.typesafe.config.ConfigValue
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader
import org.apache.kafka.common.acl.AclBinding
import org.apache.kafka.common.resource.{Resource, ResourceType}

case class KafkaToolConfig(
                            kafka: Map[String, AnyRef],
                            topicSettings: Map[String, TopicSettings],
                            brokerIds: Set[Int]
                          ) {
  def getAcls() : Array[AclBinding] = {

    def buildResource(topic: String): Resource = new Resource(ResourceType.TOPIC,topic)

    def buildAclBindings(topic: String, aclEntries: Array[TopicAclEntry]): Array[AclBinding] = {
      aclEntries
        .flatMap(aclEntry => aclEntry.toAccessControlEntries())
        .map(ACE => new AclBinding(buildResource(topic), ACE))

    }
    topicSettings
      .filter(topicSetting => (topicSetting._2.accessControlEntries.length > 0))
      .map( topicSetting => (topicSetting._1, topicSetting._2.accessControlEntries ))
      .flatMap( entry => buildAclBindings(entry._1, entry._2))
      .toArray
  }
}

object KafkaToolConfig {

  implicit val valueReader: ValueReader[KafkaToolConfig] = ValueReader.relative { config =>
    val kafka = config.as[Map[String, ConfigValue]]("kafka").mapValues(_.unwrapped())
    val defaultTopicSettings = config.as[TopicSettings]("default-topic-settings")
    val overrides = config.as[Map[String, TopicSettingsOverrides]]("topic-settings")
    val brokerIds = config.as[Seq[Int]]("broker-ids").toSet

    val topicSettings = overrides
      .mapValues(_.withDefault(defaultTopicSettings))
      .withDefaultValue(defaultTopicSettings)

    KafkaToolConfig(kafka, topicSettings, brokerIds)
  }
}

