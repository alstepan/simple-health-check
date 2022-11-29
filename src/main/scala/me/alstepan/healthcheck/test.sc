import cats.effect.IO

import java.net.URI
import cats.implicits._
import io.circe.Json.JString
import io.circe.literal._
import io.circe.syntax._
import io.circe.parser.parse
import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import io.circe.generic.semiauto._
import me.alstepan.healthcheck.repositories.inmemory.ServiceRepositoryImpl

import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.Try

case class ServiceId(value: String) extends AnyVal
case class Service(name: String)
case class Sample(id: ServiceId, name: String, uri: URI, timeout: Duration)

implicit val decoderServiceId: Decoder[ServiceId] = Decoder.decodeString.emapTry(str => Try(ServiceId(str)))
implicit val decoderUri: Decoder[URI] = Decoder.decodeString.emapTry(str => Try(URI.create(str)))
implicit val decoderDuration: Decoder[Duration] = Decoder.decodeInt.emapTry( s => Try(s.seconds))
implicit val decoderService: Decoder[Service] = deriveDecoder[Service]
implicit val decoderService: Decoder[Sample] = deriveDecoder[Sample]

import fs2._

import me.alstepan.healthcheck.services._


val e = IO.raiseError[Int](new RuntimeException("aaa"))


val sj =
  """
    |{"id": "a123"}
    |""".stripMargin
val id = sj.asJson.as[ServiceId]
println(id)

val srvj =
  """
    |{"id": "123", "name" : "Sample Service", "uri": "http://localhost:8080", "timeout":5 }
    |""".stripMargin

parse(srvj).flatMap(_.as[Sample])
srvj.asJson.as[Sample]
val s = srvj.asJson
println(s)