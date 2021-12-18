package me.alstepan.healthcheck.services

import cats.effect.Temporal
import cats.effect.implicits._
import cats.effect.kernel.Resource
import cats.implicits._
import me.alstepan.healthcheck.Domain.Services.{HealthCheckResult, Service}
import me.alstepan.healthcheck.config.HealthCheckConfig
import me.alstepan.healthcheck.repositories.{HealthCheckRepository, ServiceRepository}
import org.http4s.client.{Client, ConnectionFailure}

import java.sql.Timestamp

class HealthChecker[F[_]: Temporal](
                                    clientRes: Resource[F, Client[F]],
                                    serviceRepo: ServiceRepository[F],
                                    healthCheckRepo: HealthCheckRepository[F],
                                    config: HealthCheckConfig
                                   ) {


  def request(srv: Service): F[HealthCheckResult] =
    for {
      response <- clientRes.use { client =>
        Temporal[F].realTime.flatMap { t =>
          client
            .get(srv.uri.toString) { resp =>
              resp.bodyText.compile.string.map(r => (resp.status, r))
            }
            .timed
            .timeout(srv.maxTimeout)
            .flatMap(r =>
              HealthCheckResult(
                time = new Timestamp(t.toMillis),
                id = srv.id,
                responseTime = r._1,
                code = r._2._1.code,
                body = r._2._2
              ).pure[F])
            .recoverWith {
              case e: java.util.concurrent.TimeoutException =>
                HealthCheckResult(time = new Timestamp(t.toMillis), id = srv.id, responseTime = srv.maxTimeout, code = 408,
                  body = s"Request timeout: $e").pure[F]
              case e: ConnectionFailure =>
                HealthCheckResult(time = new Timestamp(t.toMillis), id = srv.id, responseTime = srv.maxTimeout, code = 503,
                  body = s"Cannot connect to an endpoint - service unavailable, $e").pure[F]
              case e: Throwable =>
                HealthCheckResult(time = new Timestamp(t.toMillis), id = srv.id, responseTime = srv.maxTimeout, code = 500,
                  body = s"Unhandled exception: $e").pure[F]
            }
        }
      }
    } yield response



  def runChecks: F[Nothing] = {
    (
      Temporal[F].sleep(config.scanFrequency) *>
      serviceRepo
        .list()
        .flatMap(l => Temporal[F].parTraverseN(config.parallelism)(l)(request))
        .flatMap(r => healthCheckRepo.saveCheckResults(r))
    ).foreverM
  }


}

object HealthChecker {
  def apply[F[_]: Temporal](clientRes: Resource[F, Client[F]],
                         serviceRepo: ServiceRepository[F],
                         healthCheckRepo: HealthCheckRepository[F],
                         config: HealthCheckConfig
                        ): F[HealthChecker[F]] =
    new HealthChecker[F](clientRes, serviceRepo, healthCheckRepo, config).pure[F]
}
