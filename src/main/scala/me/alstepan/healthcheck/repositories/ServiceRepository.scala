package me.alstepan.healthcheck.repositories

import cats.data.EitherT
import cats.implicits._
import cats.effect.Concurrent
import me.alstepan.healthcheck.Domain.Services.{Service, ServiceId}
import me.alstepan.healthcheck.repositories.inmemory.ServiceRepositoryImpl

trait ServiceRepository[F[_]] {

  def register(service: Service): EitherT[F, ServiceRepository.Error, Unit]
  def unregister(serviceId: ServiceId): EitherT[F, ServiceRepository.Error, Unit]
  def list(): F[List[Service]]
  def service(serviceId: ServiceId): EitherT[F, ServiceRepository.Error, Service]

}

object ServiceRepository {

  sealed trait Error extends Product with Serializable
  case class ServiceNotFound(id: ServiceId) extends Error
  case class ServiceAlreadyRegistered(id: ServiceId) extends Error

  def inMemory[F[_]: Concurrent]: F[ServiceRepository[F]] = ServiceRepositoryImpl.apply[F]

}
