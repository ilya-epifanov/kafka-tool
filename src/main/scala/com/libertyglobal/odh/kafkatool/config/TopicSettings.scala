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

import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader

import scala.collection.JavaConverters._

case class TopicSettings(
                          rf: Int,
                          partitions: Int,
                          accessControlEntries: Seq[TopicAclEntry],
                          config: Map[String, String]
                        )


object TopicSettings {
  implicit val valueReader: ValueReader[TopicSettings] = ValueReader.relative { config =>
    TopicSettings(
      config.as[Int]("rf"),
      config.as[Int]("partitions"),
      config.getOrElse[Seq[TopicAclEntry]]("access-control-entries", Seq.empty),
      if (config.hasPath("config")) {
        val c = config.getConfig("config")
        c.entrySet().asScala.map(_.getKey).map(k => k.replace("\"", "") -> c.as[String](k))(collection.breakOut)
      } else {
        Map.empty
      }
    )
  }

}
