package com.libertyglobal.odh.kafkatool.aclmanager
import com.libertyglobal.odh.kafkatool.config.{KafkaToolConfig, TopicAclSettings, TopicSettings}
import org.apache.kafka.common.acl.{AccessControlEntry, AclBinding, AclOperation, AclPermissionType}
import org.apache.kafka.common.resource.{Resource, ResourceType}
import org.apache.kafka.common.security.auth.KafkaPrincipal

class ACLManager {



  def toAclBindings(topic: String, config: TopicAclSettings): Array[AclBinding] = {

    def topicToResource(topic: String): Resource = new Resource(ResourceType.TOPIC, topic)

    for (ace <- config.toAccessControlEntries())
      yield new AclBinding(topicToResource(topic), ace)

  }


  def print(config: KafkaToolConfig): Unit = {

  }
}
