package com.programmingcentre.mediaman.admin

import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object JSONProtocol extends DefaultJsonProtocol {
  implicit val serviceStatusFormat: RootJsonFormat[ServiceStatus] = jsonFormat5(ServiceStatus)
}
