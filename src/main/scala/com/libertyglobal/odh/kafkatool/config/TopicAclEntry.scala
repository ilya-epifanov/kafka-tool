package com.libertyglobal.odh.kafkatool.config

import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader
import org.apache.kafka.common.acl.{AccessControlEntry, AclOperation, AclPermissionType}
import org.apache.kafka.common.security.auth.KafkaPrincipal

case class TopicAclEntry(principal: String, name: String,
                    hosts: Array[String], operations: Array[String],
                    permissions: Array[String]) {

  def toAccessControlEntries(): Array[AccessControlEntry] = {
    def strToOperation(op: String): AclOperation = {
      op.toUpperCase match {
        case "ANY" => AclOperation.ANY
        case "ALL" => AclOperation.ALL
        case "READ" => AclOperation.READ
        case "WRITE" => AclOperation.WRITE
        case "CREATE" => AclOperation.CREATE
        case "DELETE" => AclOperation.DELETE
        case "ALTER" => AclOperation.ALTER
        case "DESCRIBE" => AclOperation.DESCRIBE
        case "CLUSTER_ACTION" => AclOperation.CLUSTER_ACTION
        case "DESCRIBE_CONFIGS" => AclOperation.DESCRIBE_CONFIGS
        case "ALTER_CONFIGS" => AclOperation.ALTER_CONFIGS
        case "IDEMPOTENT_WRITE" => AclOperation.IDEMPOTENT_WRITE
      }
    }

    def strToPermission(p: String): AclPermissionType = {
      p.toUpperCase match {
        case "ALLOW" => AclPermissionType.ALLOW
        case "DENY" => AclPermissionType.DENY
      }
    }

    val kafkaPrincipal = new KafkaPrincipal(principal, name)

    for {
      host <- hosts;
      operation <- operations;
      permission <- permissions
    } yield new AccessControlEntry( kafkaPrincipal.toString,
                                    host, strToOperation(operation),
                                    strToPermission(permission))

  }
}

object TopicAclEntry {
  implicit val valueReader: ValueReader[TopicAclEntry] = ValueReader.relative { config =>
    TopicAclEntry(
      config.as[String]("principal"),
      config.as[String]("name"),
      config.as[Array[String]]("hosts"),
      config.as[Array[String]]("operations"),
      config.as[Array[String]]("permissions")
    )
  }
}
