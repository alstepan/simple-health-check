package me.alstepan.healthcheck.API

import cats.Applicative
import io.circe.generic.semiauto._
import io.circe.literal._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

import java.net.URI
import java.sql.Timestamp
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.Try
import fs2._
import org.http4s.{EntityEncoder, MediaType}
import org.http4s.headers._


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

  implicit def streamAsJsonArrayEncoder[F[_], T](implicit F: Applicative[F], tEncoder: Encoder[T]): EntityEncoder[F, Stream[F, T]] =
    EntityEncoder
      .streamEncoder[F, String]
      .contramap[Stream[F, T]](
        stream => Stream.emit("[") ++
        stream.map(t => t.asJson.noSpaces).intersperse(",") ++
        Stream.emit("]")
      )
}
