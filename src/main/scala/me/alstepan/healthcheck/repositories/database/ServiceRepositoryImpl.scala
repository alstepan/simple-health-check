package me.alstepan.healthcheck.repositories.database

import cats.data.EitherT
import cats.effect.Sync
import cats.effect.kernel.MonadCancelThrow
import cats.implicits._
import cats.effect.implicits._
import doobie.Transactor
import doobie.implicits._
import doobie.quill.DoobieContext
import io.getquill.{CompositeNamingStrategy2, EntityQuery, Escape, Literal}
import me.alstepan.healthcheck.Domain.Services
import me.alstepan.healthcheck.Domain.Services.{Service, ServiceId}
import me.alstepan.healthcheck.repositories.ServiceRepository
import me.alstepan.healthcheck.repositories.ServiceRepository.ServiceNotFound
import me.alstepan.healthcheck.repositories.infra.Database
import org.typelevel.log4cats.slf4j.Slf4jLogger

class ServiceRepositoryImpl[F[_]: MonadCancelThrow](tr: Transactor[F]) extends ServiceRepository[F] {

  val dc: DoobieContext.Postgres[CompositeNamingStrategy2[Escape.type, Literal.type]] = Database.doobieContext

  import me.alstepan.healthcheck.repositories.infra.DatabaseEncodings._
  import dc._

  val serviceSchema = quote {
    querySchema[Service]("services", _.maxTimeout -> "maxtimeout")
  }

  override def register(service: Services.Service): EitherT[F, ServiceRepository.Error, Unit] =
    EitherT {
      {
        for {
          _ <- serviceQuery(service.id)
          x <-
            dc
              .run(serviceSchema.insert(lift(service)))
              .map(x => ().asRight[ServiceRepository.Error])
        } yield x
      }.transact(tr)
    }


  override def unregister(serviceId: ServiceId): EitherT[F, ServiceRepository.Error, Unit] =
    EitherT {
      for {
        _ <- serviceQuery(serviceId)
        x <- dc
              .run(serviceSchema.filter(x => x.id == lift(serviceId)).delete)
              .map(_ => ().asRight[ServiceRepository.Error])
      } yield x
    }.transact(tr)

  override def list(): F[List[Services.Service]] =
    dc.run(serviceSchema).map(x => x).transact(tr)

  override def service(serviceId: Services.ServiceId): EitherT[F, ServiceRepository.Error, Services.Service] =
    EitherT {
      serviceQuery(serviceId)
        .transact(tr)
    }

  private def serviceQuery(serviceId: Services.ServiceId) =
    dc.run(serviceSchema.filter(s => s.id == lift(serviceId)))
      .map(x => x.headOption.toRight[ServiceRepository.Error](ServiceNotFound(serviceId)))
}

object ServiceRepositoryImpl {
 def apply[F[_]: Sync](tr: Transactor[F]): F[ServiceRepository[F]] = {
   for {
     logger <- Slf4jLogger.create[F]
     _ <- logger.info("Creating Doobie Service Repository")
   } yield new ServiceRepositoryImpl[F](tr)
 }
}
