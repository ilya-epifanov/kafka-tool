package com.libertyglobal.odh.kafkatool.config

import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader

case class TopicAclSettings(
                             principal: String,
                             name: String,
                             hosts: Array[String],
                             operations: Array[String],
                             permissions: Array[String]
                        )


object TopicAclSettings {
  implicit val valueReader: ValueReader[TopicAclSettings] = ValueReader.relative { config =>
    TopicAclSettings(
      config.as[String]("principal"),
      config.as[String]("name"),
      config.as[Array[String]]("hosts"),
      config.as[Array[String]]("operations"),
      config.as[Array[String]]("permissions")
    )
  }

}