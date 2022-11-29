package me.alstepan.healthcheck.repositories.database

import cats.data.Reader
import cats.effect.{MonadCancelThrow, Sync}
import cats.implicits.*
import cats.effect.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.implicits.javasql.*
import doobie.util.*
import doobie.util.update.*
import doobie.util.fragment.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.postgres.pgisimplicits.*
import me.alstepan.healthcheck.Domain.Services.{HealthCheckResult, ServiceId}
import me.alstepan.healthcheck.repositories.infra.Database
import me.alstepan.healthcheck.repositories.HealthCheckRepository
import fs2.Stream

import java.sql.Timestamp
import java.sql.Date

class HealthCheckRepositoryImpl[F[_]: MonadCancelThrow](tr: Transactor[F]) extends HealthCheckRepository[F] {

  import me.alstepan.healthcheck.repositories.infra.DatabaseEncodings.given

  override def saveCheckResults(results: Seq[HealthCheckResult]): F[Unit] = {
    val sql = "insert into public.HealthCheckResult (ID, TIME, RESPONSETIME, CODE, BODY) values (?, ?, ?, ?, ?)"
    Update[HealthCheckResult](sql).updateMany(results).transact(tr).map(_ => ())
  }

  def getResults(services: Set[ServiceId], start: Timestamp, end: Timestamp): Stream[F, HealthCheckResult] =
    {
      if (services.isEmpty) select ++  byTime(start,end)
      else select ++ byTime(start, end) ++ byService(services) 
    }.query[HealthCheckResult].stream.transact(tr) 

  override def getFailures(services: Set[ServiceId], start: Timestamp, end: Timestamp): Stream[F, HealthCheckResult] =    
    {
      if (services.isEmpty) select ++  byTime(start,end) ++ byError
      else select ++ byTime(start, end) ++ byError ++ byService(services)
    }.query[HealthCheckResult].stream.transact(tr)

  private def select = fr"select ID, TIME, RESPONSETIME, CODE, BODY from public.HealthCheckResult where "

  private def byTime(start: Timestamp, end: Timestamp) =
    fr" TIME > $start and TIME < $end"

  private def byError = fr"AND CODE >= 400"

  private def byService(services: Set[ServiceId]) =
    fr"AND ID IN (" ++ services.toList.map(id => fr0"'${id.value}'").intercalate(fr",") ++ fr")"
}

object HealthCheckRepositoryImpl {
  def apply[F[_]: Sync](tr: Transactor[F]): F[HealthCheckRepository[F]] =
    Sync[F].delay(new HealthCheckRepositoryImpl[F](tr))
}
