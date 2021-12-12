package me.alstepan.healthcheck.repositories

import cats.effect.Concurrent
import me.alstepan.healthcheck.Domain.Services.{HealthCheckResult, ServiceId}
import me.alstepan.healthcheck.repositories.inmemory.HealthCheckRepositoryImpl

import java.sql.Timestamp

trait HealthCheckRepository[F[_]] {
  def saveCheckResults(results: Seq[HealthCheckResult]): F[Unit]
  def getResults(services: Set[ServiceId], start: Timestamp, end: Timestamp): F[Seq[HealthCheckResult]]
  def getFailures(services: Set[ServiceId], start: Timestamp, end: Timestamp): F[Seq[HealthCheckResult]]

}

object HealthCheckRepository {
  def inMemory[F[_]: Concurrent]: F[HealthCheckRepository[F]] = HealthCheckRepositoryImpl.apply
}
