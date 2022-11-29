package me.alstepan.healthcheck.repositories.database

import cats.data.EitherT
import cats.effect.Sync
import cats.effect.kernel.MonadCancelThrow
import cats.implicits.*
import cats.effect.implicits.*
import doobie.Transactor
import doobie.implicits.*
import doobie.util.update.*
import doobie.free.connection.ConnectionIO
import me.alstepan.healthcheck.Domain.Services.{Service, ServiceId}
import me.alstepan.healthcheck.repositories.ServiceRepository
import me.alstepan.healthcheck.repositories.ServiceRepository.*
import me.alstepan.healthcheck.repositories.infra.Database
import org.typelevel.log4cats.slf4j.Slf4jLogger

class ServiceRepositoryImpl[F[_]: MonadCancelThrow](tr: Transactor[F]) extends ServiceRepository[F] {

  import me.alstepan.healthcheck.repositories.infra.DatabaseEncodings.given

  override def register(srv: Service): EitherT[F, ServiceRepository.Error, Unit] =
    service(srv.id)
      .flatMap(_ => EitherT{
        ServiceRepository
          .ServiceAlreadyRegistered(srv.id)
          .asInstanceOf[ServiceRepository.Error]
          .asLeft[Unit]
          .pure[F]
        }
      )
      .recoverWith {
        case ServiceRepository.ServiceNotFound(_) =>
          EitherT.right{
            sql"insert into public.Services (ID, NAME, URI, MAXTIMEOUT) values (${srv.id.value}, ${srv.name}, ${srv.uri.toASCIIString()}, ${srv.maxTimeout.toMillis})"
              .update.run.transact(tr).map(_ => ())
          }
      }

  override def unregister(serviceId: ServiceId): EitherT[F, ServiceRepository.Error, Unit] =
    EitherT {
      for {
        _ <- serviceQuery(serviceId)
        _ <- sql"delete from public.Services where ID=${serviceId.value}".update.run
      } yield Either.right[ServiceRepository.Error, Unit](())
    }.transact(tr)

  override def list(): F[List[Service]] =
    sql"select ID, NAME, URI, MAXTIMEOUT from public.Services".query[Service].to[List].transact(tr)

  override def service(serviceId: ServiceId): EitherT[F, ServiceRepository.Error, Service] =
    EitherT { serviceQuery(serviceId).transact(tr) }

  private def serviceQuery(serviceId: ServiceId) =
     sql"select ID, NAME, URI, MAXTIMEOUT from public.Services where id=${serviceId.value}"
      .query[Service]
      .unique
      .attempt
      .map(_.leftMap(_ => ServiceNotFound(serviceId).asInstanceOf[ServiceRepository.Error]))
}

object ServiceRepositoryImpl {
 def apply[F[_]: Sync](tr: Transactor[F]): F[ServiceRepository[F]] = 
   for {
     logger <- Slf4jLogger.create[F]
     _ <- logger.info("Creating Doobie Service Repository")
   } yield new ServiceRepositoryImpl[F](tr)
 
}
