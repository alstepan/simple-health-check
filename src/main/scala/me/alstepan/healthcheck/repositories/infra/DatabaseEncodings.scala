package me.alstepan.healthcheck.repositories.infra

import me.alstepan.healthcheck.Domain.Services.*
import cats.implicits.*

import java.net.URI
import java.sql.Timestamp
import java.util.Date
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import doobie.util.*

object DatabaseEncodings {

  given serviceRead: Read[Service] = 
    Read[(String, String, String, Long)].map{ (id, name, uri, timeout) => Service(ServiceId(id), name, URI.create(uri), timeout.millis) }

  given serviceWrite: Write[Service] =
    Write[(String, String, String, Long)].contramap{ s => (s.id.value, s.name, s.uri.toString, s.maxTimeout.toMillis) }

  given healthCheckResultRead: Read[HealthCheckResult] = 
    Read[(String, java.util.Date, Long, Int, String)].map{ (id, time, duration, code, body) =>
      HealthCheckResult(
        id = ServiceId(id), 
        time = new Timestamp(time.getTime()), 
        responseTime = duration.milliseconds, 
        code = code, 
        body = body
      )
    }

  given healthCheckResultWrite: Write[HealthCheckResult] =
    Write[(String, java.util.Date, Long, Int, String)].contramap {
      hr => (hr.id.value, new Date(hr.time.getTime), hr.responseTime.toMillis, hr.code, hr.body)
    }

}
