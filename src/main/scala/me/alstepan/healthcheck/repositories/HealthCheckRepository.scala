package me.alstepan.healthcheck.repositories

import cats.effect.{Concurrent, Sync}
import doobie.Transactor
import me.alstepan.healthcheck.Domain.Services.{HealthCheckResult, ServiceId}
import me.alstepan.healthcheck.repositories.inmemory.{ HealthCheckRepositoryImpl => memory }
import me.alstepan.healthcheck.repositories.database.{ HealthCheckRepositoryImpl => db }
import fs2._

import java.sql.Timestamp

trait HealthCheckRepository[F[_]] {
  def saveCheckResults(results: Seq[HealthCheckResult]): F[Unit]
  def getResults(services: Set[ServiceId], start: Timestamp, end: Timestamp): Stream[F, HealthCheckResult]
  def getFailures(services: Set[ServiceId], start: Timestamp, end: Timestamp): Stream[F, HealthCheckResult]

}

object HealthCheckRepository {
  def inMemory[F[_]: Concurrent]: F[HealthCheckRepository[F]] = memory[F]
  def doobie[F[_]: Sync](tr: Transactor[F]): F[HealthCheckRepository[F]] = db[F](tr)
}
