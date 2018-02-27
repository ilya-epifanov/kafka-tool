package com.libertyglobal.odh.kafkatool.aclmanager

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.common.acl.{AclBinding, AclBindingFilter}
import scala.collection.JavaConverters._

object ACLManager {

  def add(kafka: AdminClient, acls: Array[AclBinding]): Boolean = {
      val results = kafka.createAcls(acls.toList.asJavaCollection)
      results.all().get()
  }

  def list(kafka: AdminClient): Array[AclBinding] = {
        list(kafka, AclBindingFilter.ANY)
  }

  def list(kafka: AdminClient, filter: AclBindingFilter): Array[AclBinding] = {
    val res = kafka.describeAcls(filter)
    res.values().get().asScala.toArray
  }

  def delete(kafka: AdminClient, aclFilters: Array[AclBindingFilter] ): Array[AclBinding] = {
    val res = kafka.deleteAcls(aclFilters.toList.asJavaCollection)
    res.all().get().asScala
  }

  def deleteAll(kafka: AdminClient): Array[AclBinding] = {
    delete(kafka, Array() :+ AclBindingFilter.ANY)
  }

}
