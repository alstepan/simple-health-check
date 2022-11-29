package me.alstepan.healthcheck.repositories.db

import io.getquill.*
import me.alstepan.healthcheck.Domain.*

import java.net.URI
import java.sql.Timestamp
import java.util.Date
import scala.concurrent.duration.{DurationLong, FiniteDuration}

object Encodings:

  given MappedEncoding[URI, String] = MappedEncoding[URI, String](_.toString)
  given MappedEncoding[FiniteDuration, Long] = MappedEncoding[FiniteDuration, Long](_.toMillis)
  given MappedEncoding[Timestamp, Date] = MappedEncoding[Timestamp, java.util.Date](x => new Date(x.getTime))
  given MappedEncoding[ServiceId, String] = MappedEncoding[ServiceId, String](_.value)
  given MappedEncoding[Set[ServiceId], List[ServiceId]] = MappedEncoding[Set[ServiceId], List[ServiceId]](_.toList)
  given MappedEncoding[String, URI] = MappedEncoding[String, URI](URI.create)
  given MappedEncoding[Long, FiniteDuration] = MappedEncoding[Long, FiniteDuration](_.milliseconds)
  given MappedEncoding[Date, Timestamp] = MappedEncoding[java.util.Date, Timestamp](x => Timestamp.from(x.toInstant))
  given MappedEncoding[String, ServiceId] = MappedEncoding[String, ServiceId](ServiceId.apply)
  
  extension (left: Timestamp)
    def >(right: Timestamp) = quote(infix"$left > $right".as[Boolean])
    def <(right: Timestamp) = quote(infix"$left < $right".as[Boolean])


