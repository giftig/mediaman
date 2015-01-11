package com.programmingcentre.mediaman.admin


case class ServiceStatus(
  service_name: String,
  service_port: Int,
  admin_port: Int,
  uptime: Long,
  pid: Option[Int]
)
