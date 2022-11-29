package me.alstepan.healthcheck.API

import java.net.URI
import java.sql.Timestamp
import scala.util.{Failure, Left, Right, Success, Try}
import scala.concurrent.duration.{FiniteDuration, DurationLong}

import zio.json.*
import me.alstepan.healthcheck.Domain.{Service, ServiceId}

object JsonFormats:
  given JsonDecoder[URI] = JsonDecoder[String].mapOrFail { v =>
    Try(URI.create(v)) match {
      case Success(v) => Right(v)
      case Failure(ex) => Left(s"Cannot parse URL: $ex")
    }
  }
  given JsonDecoder[FiniteDuration] = JsonDecoder[Long].map(_.millis)
  given JsonDecoder[ServiceId] = JsonDecoder[String].map(v => ServiceId(v))
  given JsonDecoder[Service] = DeriveJsonDecoder.gen[Service]

  given JsonEncoder[URI] = JsonEncoder[String].contramap(_.toString)
  given JsonEncoder[FiniteDuration] = JsonEncoder[Long].contramap(_.toMillis)
  given JsonEncoder[ServiceId] = JsonEncoder[String].contramap(_.value)
  given JsonEncoder[Service] = DeriveJsonEncoder.gen[Service]

