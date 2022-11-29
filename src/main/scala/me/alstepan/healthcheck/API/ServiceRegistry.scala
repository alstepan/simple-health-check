package me.alstepan.healthcheck.API

import zio.*
import zio.stream.*
import zhttp.http.*
import zio.json.*
import me.alstepan.healthcheck.repositories.*
import me.alstepan.healthcheck.Domain.{Service, ServiceId}

object ServiceRegistry {

  import JsonFormats.given

  def register: Http[ServiceRepository, Nothing, Request, Response] = Http.collectZIO[Request] {
    case req@ Method.POST -> !! / "registry" / "add" =>
      (for {
        srv <- req.body.asString.flatMap(b => ZIO.fromEither(b.fromJson[Service]))
        _ <- ServiceRepository.register(srv)
      } yield Response.ok).orElse( ZIO.succeed(Response.status(Status.BadRequest)))
  }

  def list: Http[ServiceRepository, Nothing, Request, Response] = Http.collectHttp[Request] {
    case Method.GET -> !! / "registry" / "list" =>
      Http.fromStream(
        ZStream.fromIterable("[".getBytes(HTTP_CHARSET))
          ++ ServiceRepository
              .list()
              .map(_.toJson)
              .intersperse(",")
              .flatMap(s => ZStream.fromIterable(s.getBytes(HTTP_CHARSET)))
          ++ ZStream.fromIterable("]".getBytes(HTTP_CHARSET))
      )
  }

  def service: Http[ServiceRepository, Nothing, Request, Response] = Http.collectZIO[Request] {
    case Method.GET -> !! / "registry" / "service" / id =>
      (for {
        s <- ServiceRepository.service(ServiceId(id))
      } yield Response.json(s.toJson)).orElse(ZIO.succeed(Response.status(Status.NotFound)))
  }

  def unregister: Http[ServiceRepository, Nothing, Request, Response] = Http.collectZIO[Request] {
    case Method.DELETE -> !! / "registry" / "remove" / id =>
      (for {
        s <- ServiceRepository.unregister(ServiceId(id))
      } yield Response.status(Status.NoContent)).orElse(ZIO.succeed(Response.status(Status.NotFound)))

  }

  def all  = register ++ list ++ service ++ unregister
}
