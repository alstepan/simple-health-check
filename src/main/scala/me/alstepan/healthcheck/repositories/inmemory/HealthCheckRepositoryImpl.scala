package me.alstepan.healthcheck.repositories.inmemory

import cats.effect.{Concurrent, Ref}
import cats.implicits._
import me.alstepan.healthcheck.Domain.Services.{HealthCheckResult, ServiceId}
import me.alstepan.healthcheck.repositories.HealthCheckRepository

import java.sql.Timestamp

class HealthCheckRepositoryImpl[F[_]: Concurrent](repo: Ref[F, List[HealthCheckResult]]) extends HealthCheckRepository[F] {
  override def saveCheckResults(results: Seq[HealthCheckResult]): F[Unit] =
    repo.update(l => (l ++ results).takeRight(1000))

  override def getResults(services: Set[ServiceId], start: Timestamp, end: Timestamp): F[List[HealthCheckResult]] =
    repo.get.map(l =>
      l.filter(r => r.time.after(start) && r.time.before(end) && (services.isEmpty || services.contains(r.id) ))
    )

  override def getFailures(services: Set[ServiceId], start: Timestamp, end: Timestamp): F[List[HealthCheckResult]] =
    getResults(services, start, end).map(l => l.filter(r => r.code >= 400))
}

object HealthCheckRepositoryImpl {
  def apply[F[_]: Concurrent]: F[HealthCheckRepository[F]] =
    for {
      results <- Ref.of[F, List[HealthCheckResult]](List())
    } yield new HealthCheckRepositoryImpl[F](results)
}
