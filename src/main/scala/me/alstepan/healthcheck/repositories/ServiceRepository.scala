package me.alstepan.healthcheck.repositories

import zio.*
import zio.stream.*
import me.alstepan.healthcheck.Domain.{Service, ServiceId}

sealed trait Error extends Product with Serializable
case class ServiceNotFound(id: ServiceId) extends Error
case class ServiceAlreadyRegistered(id: ServiceId) extends Error
case class RepositoryError(cause: String) extends Error

trait ServiceRepository:

  def register(service: Service): IO[Error, Unit]
  def unregister(serviceId: ServiceId): IO[Error, Unit]
  def list(): ZStream[Any, Throwable, Service]
  def service(serviceId: ServiceId): IO[Error, Service]



object ServiceRepository:
  def register(service: Service): ZIO[ServiceRepository, Error, Unit] =
    ZIO.serviceWithZIO[ServiceRepository](_.register(service))

  def unregister(serviceId: ServiceId): ZIO[ServiceRepository, Error, Unit] =
    ZIO.serviceWithZIO[ServiceRepository](_.unregister(serviceId))

  def list(): ZStream[ServiceRepository, Throwable, Service] =
    ZStream.serviceWithStream[ServiceRepository](_.list())

  def service(serviceId: ServiceId): ZIO[ServiceRepository, Error, Service] =
    ZIO.serviceWithZIO[ServiceRepository](_.service(serviceId))


