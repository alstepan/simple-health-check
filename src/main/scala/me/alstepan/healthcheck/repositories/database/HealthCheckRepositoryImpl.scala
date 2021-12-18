package me.alstepan.healthcheck.repositories.database

import cats.data.Reader
import cats.effect.{MonadCancelThrow, Sync}
import cats.implicits._
import cats.effect.implicits._
import doobie.Transactor
import doobie.implicits._
import doobie.quill.DoobieContext
import io.getquill.{CompositeNamingStrategy2, EntityQuery, Escape, Literal}
import me.alstepan.healthcheck.Domain.Services.{HealthCheckResult, ServiceId}
import me.alstepan.healthcheck.repositories.infra.Database
import me.alstepan.healthcheck.repositories.HealthCheckRepository
import fs2.Stream

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


  def getResults(services: Set[ServiceId], start: Timestamp, end: Timestamp): Stream[F, HealthCheckResult] =
    {
      if (services.isEmpty) dc.stream { byTime(start,end).run(resultSchema) }
      else dc.stream { byService(services).andThen(byTime(start, end)).run(resultSchema) }
    }.map(x => x).transact(tr)

  override def getFailures(services: Set[ServiceId], start: Timestamp, end: Timestamp): Stream[F, HealthCheckResult] =
    {
      if (services.isEmpty) dc.stream { byTime(start,end).andThen(byError).run(resultSchema) }
      else dc.stream { byService(services).andThen(byTime(start, end)).andThen(byError).run(resultSchema) }
    }.map(x=> x).transact(tr)

  private def byTime(start: Timestamp, end: Timestamp) =
    Reader { (query: Quoted[EntityQuery[HealthCheckResult]]) =>
      quote { query.filter(r => r.time > lift(start)).filter(r => r.time < lift(end)) }
    }

  private def byError =
    Reader { (query: Quoted[EntityQuery[HealthCheckResult]]) =>
      quote { query.filter(r => r.code >= lift(400)) }
    }

  private def byService(services: Set[ServiceId]) =
    Reader { (query: Quoted[EntityQuery[HealthCheckResult]] )=>
      quote { query.filter(r => liftQuery(services).contains(r.id)) }
    }

}

object HealthCheckRepositoryImpl {
  def apply[F[_]: Sync](tr: Transactor[F]): F[HealthCheckRepository[F]] =
    Sync[F].delay(new HealthCheckRepositoryImpl[F](tr))
}
