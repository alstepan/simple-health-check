package me.alstepan.healthcheck.API

import cats.effect.Temporal
import cats.implicits._
import io.circe.syntax._
import me.alstepan.healthcheck.Domain.Services.ServiceId
import me.alstepan.healthcheck.repositories.HealthCheckRepository
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, QueryParamDecoder}

import java.sql.Timestamp

class Statistics[F[_]: Temporal](healthStatRepo: HealthCheckRepository[F]) {
  object dsl extends Http4sDsl[F]
  import dsl._
  import me.alstepan.healthcheck.API.JsonFormats._

  implicit val servicesQueryParamDecoder: QueryParamDecoder[Set[String]] =
    QueryParamDecoder[String].map(s => s.split(",").toSet)

  implicit val timeQueryParamDecoder: QueryParamDecoder[Timestamp] =
    QueryParamDecoder[String].map(s => Timestamp.valueOf(s))


  object Services extends OptionalQueryParamDecoderMatcher[Set[String]]("services")
  object StartTime extends OptionalQueryParamDecoderMatcher[Timestamp]("start")
  object EndTime extends OptionalQueryParamDecoderMatcher[Timestamp]("end")

  private def servicesStat: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> root / "query" :? Services(servicesIds) +& StartTime(start) +& EndTime(end) =>
      for {
        time <- Temporal[F].realTime
        results <- healthStatRepo
          .getResults(servicesIds.map(s => s.map(ServiceId)).getOrElse(Set()),
            start.getOrElse(new Timestamp(0)), end.getOrElse(new Timestamp(time.toMillis))
          )
        resp <- Ok(results.asJson.toString)
      } yield resp
  }

  private def failuresStat: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> root / "failures" :? Services(servicesIds) +& StartTime(start) +& EndTime(end) =>
      for {
        time <- Temporal[F].realTime
        results <- healthStatRepo
          .getFailures(servicesIds.map(s => s.map(ServiceId)).getOrElse(Set()),
            start.getOrElse(new Timestamp(0)), end.getOrElse(new Timestamp(time.toMillis))
          )
        resp <- Ok(results.asJson.toString)
      } yield resp
  }

  def endpoints: HttpRoutes[F] = servicesStat <+> failuresStat
}

object Statistics {

  def endpoints[F[_]: Temporal](healthCheckRepo: HealthCheckRepository[F]): HttpRoutes[F] =
    new Statistics[F](healthCheckRepo).endpoints

}
