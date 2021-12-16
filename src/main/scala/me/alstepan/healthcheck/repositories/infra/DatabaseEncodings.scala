package me.alstepan.healthcheck.repositories.infra

import io.getquill.MappedEncoding
import io.getquill.mirrorContextWithQueryProbing.{InfixInterpolator, quote}
import me.alstepan.healthcheck.Domain.Services.ServiceId

import java.net.URI
import java.sql.Timestamp
import java.util.Date
import scala.concurrent.duration.{DurationLong, FiniteDuration}

object DatabaseEncodings {

  implicit val encodeURI = MappedEncoding[URI, String](_.toString)
  implicit val encodeDuration = MappedEncoding[FiniteDuration, Long](_.toMillis)
  implicit val encodeTimestamp = MappedEncoding[Timestamp, java.util.Date](x => new Date(x.getTime))
  implicit val encodeServiceId = MappedEncoding[ServiceId, String](_.value)
  implicit val encodeSet = MappedEncoding[Set[ServiceId], List[ServiceId]](_.toList)
  implicit val decodeURI = MappedEncoding[String, URI](URI.create)
  implicit val decodeDuration = MappedEncoding[Long, FiniteDuration](_.milliseconds)
  implicit val decodeTimestamp = MappedEncoding[java.util.Date, Timestamp](x => Timestamp.from(x.toInstant))
  implicit val decodeServiceId = MappedEncoding[String, ServiceId](ServiceId.apply)
  implicit class DateTimeQuotes(left: Timestamp) {
    def >(right: Timestamp) = quote(infix"$left > $right".as[Boolean])
    def <(right: Timestamp) = quote(infix"$left < $right".as[Boolean])
  }

}
