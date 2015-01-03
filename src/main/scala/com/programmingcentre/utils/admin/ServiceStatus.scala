package com.programmingcentre.utils.admin


case class ServiceStatus(
  service_name: String,
  service_port: Int,
  admin_port: Int,
  uptime: Long,
  pid: Int
)
