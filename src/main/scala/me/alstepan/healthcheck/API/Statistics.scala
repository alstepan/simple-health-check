package me.alstepan.healthcheck.API

import cats.effect.Temporal
import cats.data._
import cats.implicits._
import fs2.Stream
import me.alstepan.healthcheck.Domain.Services.{HealthCheckResult, ServiceId}
import me.alstepan.healthcheck.repositories.HealthCheckRepository
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, ParseFailure, QueryParamDecoder, Response}

import java.sql.Timestamp

class Statistics[F[_]: Temporal](healthStatRepo: HealthCheckRepository[F]) {
  object dsl extends Http4sDsl[F]
  import dsl._
  import me.alstepan.healthcheck.API.JsonFormats._

  implicit val servicesQueryParamDecoder: QueryParamDecoder[Set[String]] =
    QueryParamDecoder[String].map(s => s.split(",").toSet)

  implicit val timeQueryParamDecoder: QueryParamDecoder[Timestamp] =
    QueryParamDecoder[String].emap(s =>
      Either
        .catchNonFatal(Timestamp.valueOf(s))
        .leftMap(_ => ParseFailure(s, s"Cannot convert $s to Timestamp"))
    )

  object Services extends OptionalQueryParamDecoderMatcher[Set[String]]("services")
  object StartTime extends  OptionalValidatingQueryParamDecoderMatcher[Timestamp]("start")
  object EndTime extends OptionalValidatingQueryParamDecoderMatcher[Timestamp]("end")

  private def servicesStat: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> _ / "query" :? Services(servicesIds) +& StartTime(start) +& EndTime(end) =>
      processRequest(servicesIds, start, end, healthStatRepo.getResults)
  }

  private def failuresStat: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> _ / "failures" :? Services(servicesIds) +& StartTime(start) +& EndTime(end) =>
      processRequest(servicesIds, start, end, healthStatRepo.getFailures)
  }

  private def processRequest(servicesIds: Option[Set[String]],
                             start: Option[ValidatedNel[ParseFailure, Timestamp]],
                             end: Option[ValidatedNel[ParseFailure, Timestamp]],
                             func: (Set[ServiceId], Timestamp, Timestamp) => Stream[F, HealthCheckResult]
                            ): F[Response[F]] =
    for {
      time <- Temporal[F].realTime
      srv = servicesIds.map(s => s.map(ServiceId)).getOrElse(Set())
      st = start.getOrElse(Validated.validNel(new Timestamp(0)))
      ed = end.getOrElse(Validated.validNel(new Timestamp(time.toMillis)))
      results = for {
        s <- st.toEither
        e <- ed.toEither
      } yield func(srv, s, e)
      resp <- results.fold(
        _ => BadRequest("Cannot parse parameters: start and end must be valid timestamp YYYY-MM-DD hh:mm:ss"),
        resp => Ok(resp)
      )
    } yield resp

  def endpoints: HttpRoutes[F] = servicesStat <+> failuresStat
}

object Statistics {

  def endpoints[F[_]: Temporal](healthCheckRepo: HealthCheckRepository[F]): HttpRoutes[F] =
    new Statistics[F](healthCheckRepo).endpoints

}
