package me.alstepan.healthcheck.repositories.inmemory

import cats.data.EitherT
import cats.implicits._
import cats.effect.implicits._
import cats.effect.{Concurrent, Ref, Sync}
import me.alstepan.healthcheck.Domain.Services
import me.alstepan.healthcheck.Domain.Services.{Service, ServiceId}
import me.alstepan.healthcheck.repositories.ServiceRepository
import me.alstepan.healthcheck.repositories.ServiceRepository.ServiceNotFound
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.net.URI
import scala.concurrent.duration.DurationInt


class ServiceRepositoryImpl[F[_]: Concurrent](services: Ref[F, Map[ServiceId, Service]]) extends ServiceRepository[F] {
  override def register(srv: Services.Service): EitherT[F, ServiceRepository.Error, Unit] =
    for {
      y <- service(srv.id)
        .map(_ => ())
        .recoverWith {
          case ServiceRepository.ServiceNotFound(_) =>
            EitherT.right(services.update(m => m ++ Map(srv.id -> srv)))
        }
    } yield y

  override def unregister(serviceId: ServiceId): EitherT[F, ServiceRepository.Error, Unit] =
    for {
      _ <- service(serviceId)
      x <- EitherT(services.update(m => m.removed(serviceId)).map(_.asRight[ServiceRepository.Error]))
    } yield x

  override def list(): F[List[Services.Service]] = services.get.map(m => m.values.toList)

  override def service(serviceId: ServiceId): EitherT[F, ServiceRepository.Error, Service] =
    for {
      s <- EitherT(services
        .get
        .map(m => m.get(serviceId).toRight[ServiceRepository.Error](ServiceNotFound(serviceId))))
    } yield s
}

object ServiceRepositoryImpl {
  def apply[F[_]: Concurrent]: F[ServiceRepository[F]] =
    for {
      services <- Ref.of[F, Map[ServiceId, Service]](Map())
      impl = new ServiceRepositoryImpl[F](services)
      _ <- impl
        .register(Service(ServiceId("srvid123"), "Sample Service", URI.create("http://localhost:8086"), 5.seconds))
        .foldF(err => Concurrent[F].unit, succ => Concurrent[F].unit)
    } yield impl
}
