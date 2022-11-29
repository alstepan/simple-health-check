package me.alstepan.healthcheck.repositories.inmemory

import zio.*
import me.alstepan.healthcheck.repositories.*
import me.alstepan.healthcheck.repositories.ServiceRepository
import me.alstepan.healthcheck.Domain.{Service, ServiceId}
import zio.stream.ZStream

import java.net.URI
import scala.concurrent.duration.DurationInt

case class ServiceRepositoryImpl(services: Ref[Map[ServiceId, Service]]) extends ServiceRepository:
  override def register(service: Service): IO[Error, Unit] =
    for {
      srvs <- services.get
      _ <- ZIO.cond(!srvs.contains(service.id), srvs, ServiceAlreadyRegistered(service.id))
      _ <- services.update(srv => srv + (service.id -> service))
    } yield()

  override def unregister(serviceId: ServiceId): IO[Error, Unit] =
    for {
      srvs <- services.get
      _ <- ZIO.cond(srvs.contains(serviceId), srvs, ServiceNotFound(serviceId))
      _ <- services.getAndUpdate(srv => srv - serviceId)
    } yield ()

  override def list(): ZStream[Any, Throwable, Service] =
    ZStream.fromIterableZIO {
      services
        .get
        .map(m => m.values)
    }


  override def service(serviceId: ServiceId): IO[Error, Service] =
    services
      .get
      .flatMap{srvs =>
        ZIO
          .fromOption(srvs.get(serviceId))
          .orElseFail(ServiceNotFound(serviceId))
      }


object ServiceRepositoryImpl:
  val layer: ZLayer[Any, Nothing, ServiceRepositoryImpl] =
    ZLayer {
      (for {
        srv <- Ref.make(Map[ServiceId, Service]())
        _ <- srv.update(_ => Map(ServiceId("asd") -> Service(ServiceId("asd"), "qwe", URI.create("http://localhost"), 3.millis)))
      } yield ServiceRepositoryImpl(srv)).debug("Initialized ref")
    }

