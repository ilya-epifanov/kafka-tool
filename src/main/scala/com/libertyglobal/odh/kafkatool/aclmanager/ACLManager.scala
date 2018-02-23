package com.libertyglobal.odh.kafkatool.aclmanager
import com.libertyglobal.odh.kafkatool.config.KafkaToolConfig
import org.apache.kafka.common.acl.{AccessControlEntry, AclOperation, AclPermissionType}
import org.apache.kafka.common.security.auth.KafkaPrincipal

class ACLManager {

  def apply(aclEntries: Array[AccessControlEntry]): Boolean = {
    true
  }

  def add(aclEntry: AccessControlEntry): Boolean = {
    true
  }
  def remove() {}
  def list() {}
  def removeAll(){}


  def print(config: KafkaToolConfig): Unit = {
      config.topicSettings foreach println
  }
}
