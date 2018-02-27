package com.libertyglobal.odh.kafkatool.aclmanager

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.common.acl.{AclBinding, AclBindingFilter}

object ACLManager {

  def add(kafka: AdminClient, acls: Array[AclBinding]): Boolean = {
    false
  }

  def list(kafka: AdminClient): Array[AclBinding] = {
    Array.empty
  }

  def delete(kafka: AdminClient, aclFilters: Array[AclBindingFilter] ): Array[AclBinding] = {
    Array.empty

  }

  def deleteAll(kafka: AdminClient): Array[AclBinding] = {
    delete(kafka, Array() :+ AclBindingFilter.ANY)
  }

}
