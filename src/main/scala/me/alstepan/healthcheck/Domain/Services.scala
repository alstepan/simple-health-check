package me.alstepan.healthcheck.Domain

import java.net.URI
import java.sql.Timestamp
import scala.concurrent.duration.FiniteDuration


case class ServiceId(value: String) extends AnyVal
case class Service(id: ServiceId, name: String, uri: URI, maxTimeout: FiniteDuration)
case class HealthCheckResult(time: Timestamp, id: ServiceId, responseTime: FiniteDuration, code: Int, body: String)



