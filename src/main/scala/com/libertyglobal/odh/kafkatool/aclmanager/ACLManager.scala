package com.libertyglobal.odh.kafkatool.aclmanager

import java.util.concurrent.{ExecutionException, TimeUnit}

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.common.acl.{AclBinding, AclBindingFilter}

import scala.collection.JavaConverters._

object ACLManager {

  def add(kafka: AdminClient, acls: Seq[AclBinding], dryRun: Boolean = false): Boolean = {
    try {
      if (!dryRun) {
        kafka.createAcls(acls.asJavaCollection).all().get()
      }
      true
    } catch {
      case ee: ExecutionException => return false
    }
  }

  def list(kafka: AdminClient): Seq[AclBinding] = {
        list(kafka, AclBindingFilter.ANY)
  }

  def list(kafka: AdminClient, filter: AclBindingFilter): Seq[AclBinding] = {
    val res = kafka.describeAcls(filter)
    res.values().get().asScala.toSeq
  }

  def delete(kafka: AdminClient, aclFilters: Seq[AclBindingFilter] ): Seq[AclBinding] = {
    val res = kafka.deleteAcls(aclFilters.asJavaCollection)
    res.all().get().asScala.toSeq
  }

  def deleteAll(kafka: AdminClient): Seq[AclBinding] = {
    delete(kafka, Seq() :+ AclBindingFilter.ANY)
  }

}
