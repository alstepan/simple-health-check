package me.alstepan.healthcheck.repositories.db

import zio.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import me.alstepan.healthcheck.Domain.*
import me.alstepan.healthcheck.repositories
import me.alstepan.healthcheck.repositories.*
import zio.stream.ZStream

import javax.sql.DataSource

class ServiceRepositoryImpl(quill: Quill.Postgres[LowerCase]) extends ServiceRepository:
  import Encodings.given
  import quill.*

  inline def serviceSchema: Quoted[EntityQuery[Service]] = quote {
    querySchema[Service]("services", _.maxTimeout -> "maxtimeout")
  }

  override def list(): ZStream[Any, Throwable, Service] =
    stream(serviceSchema)

  override def service(serviceId: ServiceId): IO[repositories.Error, Service] =
    run(
      serviceSchema.filter(srv => srv.id == lift(serviceId)).take(1)
    ).flatMap(h => ZIO.fromOption(h.headOption))
     .orElseFail(ServiceNotFound(serviceId).asInstanceOf[repositories.Error])

  override def register(service: Service): IO[repositories.Error, Unit] =
    run(serviceSchema.insertValue(lift(service)))
      .mapError(e => RepositoryError(e.getMessage))
      .flatMap(c => ZIO.cond(c > 0, (), ServiceAlreadyRegistered(service.id).asInstanceOf[repositories.Error]))

  override def unregister(serviceId: ServiceId): IO[repositories.Error, Unit] =
    run(serviceSchema.filter(srv => srv.id == lift(serviceId)).delete)
      .mapError( e =>  RepositoryError(e.getMessage) )
      .flatMap { c => ZIO.cond( c > 0, (), ServiceNotFound(serviceId).asInstanceOf[repositories.Error]) }


object ServiceRepositoryImpl:
  val layer = ZLayer.fromFunction(new ServiceRepositoryImpl(_))

