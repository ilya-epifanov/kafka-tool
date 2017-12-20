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

package com.libertyglobal.odh.kafkatool.partitionreassignment

import com.libertyglobal.odh.kafkatool.BrokerId

case class PartitionsPerBroker(distribution: Map[BrokerId, Int]) extends AnyVal {
  def apply(id: BrokerId): Int = distribution(id)

  def inc(id: BrokerId): PartitionsPerBroker = {
    PartitionsPerBroker(distribution.updated(id, distribution(id) + 1))
  }

  def dec(id: BrokerId): PartitionsPerBroker = {
    PartitionsPerBroker(distribution.updated(id, distribution(id) - 1))
  }

  def minReplicas(blacklist: Iterable[BrokerId] = List.empty): Int = {
    (distribution -- blacklist).values.min
  }

  def minReplicasFromWhitelist(whitelist: Set[BrokerId] = Set.empty): Int = {
    distribution.filterKeys(whitelist.contains).values.min
  }

  def minReplicasBrokers(blacklist: Set[BrokerId] = Set.empty): Seq[BrokerId] = {
    val replicas = minReplicas(blacklist)
    distribution.toSeq.filter({ case (b, r) => r == replicas && !blacklist.contains(b) }).map(_._1)
  }

  def minReplicasBrokersFromWhitelist(whitelist: Set[BrokerId]): Set[BrokerId] = {
    val allowedBrokers = distribution.filterKeys(whitelist.contains)
    val minReplicas = allowedBrokers.values.min
    allowedBrokers.collect({
      case (b, r) if r == minReplicas =>
        b
    })(collection.breakOut)
  }

  def maxReplicasBrokersFromWhitelist(whitelist: Set[BrokerId]): Set[BrokerId] = {
    val allowedBrokers = distribution.filterKeys(whitelist.contains)
    val maxReplicas = allowedBrokers.values.max
    allowedBrokers.collect({
      case (b, r) if r == maxReplicas =>
        b
    })(collection.breakOut)
  }
}
