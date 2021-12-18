package me.alstepan.healthcheck.repositories.database

import cats.effect.{MonadCancelThrow, Sync}
import cats.implicits._
import cats.effect.implicits._
import doobie.Transactor
import doobie.implicits._
import doobie.quill.DoobieContext
import io.getquill.{CompositeNamingStrategy2, Escape, Literal}
import me.alstepan.healthcheck.Domain.Services.{HealthCheckResult, ServiceId}
import me.alstepan.healthcheck.repositories.infra.Database
import me.alstepan.healthcheck.repositories.HealthCheckRepository

import java.sql.Timestamp

class HealthCheckRepositoryImpl[F[_]: MonadCancelThrow](tr: Transactor[F]) extends HealthCheckRepository[F] {

  val dc: DoobieContext.Postgres[CompositeNamingStrategy2[Escape.type, Literal.type]] = Database.doobieContext

  import me.alstepan.healthcheck.repositories.infra.DatabaseEncodings._
  import dc._

  val resultSchema = quote {
    querySchema[HealthCheckResult]("healthcheckresult", _.responseTime -> "responsetime")
  }

  override def saveCheckResults(results: Seq[HealthCheckResult]): F[Unit] =
    dc.run { liftQuery(results).foreach(r => resultSchema.insert(r).onConflictIgnore) }.map(_ => ()).transact(tr)


  def getResults(services: Set[ServiceId], start: Timestamp, end: Timestamp): F[List[HealthCheckResult]] =
    {
      if (services.isEmpty)
        dc.run {
          resultSchema
            .filter(r => r.time > lift(start))
            .filter(r => r.time < lift(end))
        }
      else
        dc.run {
          resultSchema
            .filter(r => liftQuery(services).contains(r.id))
            .filter(r => r.time > lift(start))
            .filter(r => r.time < lift(end))
        }
    }.map(x=> x).transact(tr)

  override def getFailures(services: Set[ServiceId], start: Timestamp, end: Timestamp): F[List[HealthCheckResult]] =
    {
      if (services.isEmpty)
        dc.run {
          resultSchema
            .filter(r => r.time > lift(start))
            .filter(r => r.time < lift(end))
            .filter(r => r.code >= lift(400))
        }
      else
        dc.run {
          resultSchema
            .filter(r => liftQuery(services).contains(r.id))
            .filter(r => r.time > lift(start))
            .filter(r => r.time < lift(end))
            .filter(r => r.code >= lift(400))
        }
    }.map(x=> x).transact(tr)


}

object HealthCheckRepositoryImpl {
  def apply[F[_]: Sync](tr: Transactor[F]): F[HealthCheckRepository[F]] =
    Sync[F].delay(new HealthCheckRepositoryImpl[F](tr))
}
