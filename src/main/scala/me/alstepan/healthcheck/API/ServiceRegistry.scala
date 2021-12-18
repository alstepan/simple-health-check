package me.alstepan.healthcheck.API

import cats.effect.Concurrent
import cats.implicits._
import io.circe.Encoder._
import io.circe.parser.parse
import io.circe.syntax._
import me.alstepan.healthcheck.Domain.Services._
import me.alstepan.healthcheck.repositories.ServiceRepository
import org.http4s._
import org.http4s.dsl.Http4sDsl

class ServiceRegistry[F[_]: Concurrent](serviceRepo: ServiceRepository[F]) {

  object dsl extends Http4sDsl[F]
  import dsl._
  import me.alstepan.healthcheck.API.JsonFormats._

  private def registerService: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> root / "add" =>
      for {
        srvs <- req.bodyText.compile.string.flatMap(s => parse(s).flatMap(_.as[Service]).pure[F])
        resp <- srvs.fold(
          err => BadRequest(s"Cannot parse request body: $err"),
          srv => serviceRepo
            .register(srv)
            .foldF(
              err => NotAcceptable(s"Service already registered: $err"),
              _ => Ok()
            )
        )
      } yield resp
  }

  private def unregisterService: HttpRoutes[F] = HttpRoutes.of[F] {
    case  DELETE -> root / "remove" / id =>
      serviceRepo
        .unregister(ServiceId(id))
        .foldF(e => NotFound(s"Service $id was not found"), _ => NoContent())
  }

  private def listServices: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> root / "list" =>
      serviceRepo
        .list()
        .flatMap(s => Ok(s.asJson.toString()))
  }

  private def serviceDetails: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> root / "service" / id =>
      serviceRepo
        .service(ServiceId(id))
        .foldF(e => NotFound(s"Service $id was not found"), srv => Ok(srv.asJson.toString()))
  }

  def endpoints = {
    registerService <+> unregisterService <+> listServices <+> serviceDetails
  }

}

object ServiceRegistry {
  def endpoints[F[_]: Concurrent](serviceRepo: ServiceRepository[F]) =
    new ServiceRegistry[F](serviceRepo).endpoints
}
