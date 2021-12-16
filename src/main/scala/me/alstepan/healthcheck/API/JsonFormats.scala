package me.alstepan.healthcheck.API

import io.circe.generic.semiauto._
import io.circe.literal._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

import java.net.URI
import java.sql.Timestamp
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.Try


object JsonFormats {

  import me.alstepan.healthcheck.Domain.Services._

  implicit val decoderServiceId: Decoder[ServiceId] = Decoder.decodeString.emapTry(str => Try(ServiceId(str)))
  implicit val decoderUri: Decoder[URI] = Decoder.decodeString.emapTry(str => Try(URI.create(str)))
  implicit val decoderDuration: Decoder[FiniteDuration] = Decoder.decodeInt.emapTry( s => Try(s.millis))
  implicit val decoderTimestamp: Decoder[Timestamp] = Decoder.decodeString.emapTry( s => Try(Timestamp.valueOf(s)))
  implicit val decoderService: Decoder[Service] = deriveDecoder[Service]
  implicit val decoderCheckResult: Decoder[HealthCheckResult] = deriveDecoder[HealthCheckResult]

  implicit val encoderServiceId: Encoder[ServiceId] = (a: ServiceId) => a.value.asJson
  implicit val encoderUri: Encoder[URI] = (u: URI) => u.toString.asJson
  implicit val encoderDuration: Encoder[FiniteDuration] = (d: FiniteDuration) => d.toMillis.asJson
  implicit val encoderTimestamp: Encoder[Timestamp] = (d: Timestamp) => d.toString.asJson
  implicit val encoderService: Encoder[Service] = deriveEncoder[Service]
  implicit val encoderCheckResult: Encoder[HealthCheckResult] = deriveEncoder[HealthCheckResult]

}
