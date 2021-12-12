package me.alstepan.healthcheck

import cats._
import cats.data._
import cats.implicits._
import cats.effect.implicits._
import cats.effect.{Async, ExitCode, IO, IOApp, Resource}
import io.circe.syntax.EncoderOps
import me.alstepan.healthcheck.API.{ServiceRegistry, Statistics}
import me.alstepan.healthcheck.config.{AppConfig, HealthCheckConfig, ServerConfig}
import me.alstepan.healthcheck.repositories.{HealthCheckRepository, ServiceRepository}
import org.http4s.blaze.client.BlazeClientBuilder
import me.alstepan.healthcheck.services.HealthChecker
import org.http4s.implicits._
import org.http4s.server.{Router, Server}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.middleware._
import org.http4s.server.staticcontent._
import pureconfig._
import pureconfig.generic.auto._

import scala.concurrent.duration.DurationInt

object Server extends IOApp {

  val defaultConfig: AppConfig = AppConfig(
    serverConf = ServerConfig("0.0.0.0", 8080),
    checkerConf = HealthCheckConfig(5.seconds, 5)
  )

  val cors = CORS.policy.withAllowOriginAll

  def createServer[F[_]: Async]: Resource[F, (Server, HealthChecker[F])] =
    for {
      config <- Resource.eval(
        Async[F].delay(
          ConfigSource
            .default
            .load[AppConfig]
            .getOrElse(defaultConfig)
        )
      )
      serviceRepo <- Resource.eval(ServiceRepository.inMemory[F])
      healthCheckRepo <- Resource.eval(HealthCheckRepository.inMemory[F])
      httpApp = cors.apply(
        Router(
          "registry" -> ServiceRegistry.endpoints[F](serviceRepo),
          "stat" -> Statistics.endpoints[F](healthCheckRepo),
          "monitor" -> resourceServiceBuilder[F]("/assets").toRoutes
        ).orNotFound
      )
      checker <- Resource.eval(
        HealthChecker.apply[F](BlazeClientBuilder[F].resource, serviceRepo, healthCheckRepo, config.checkerConf))
      server <- BlazeServerBuilder[F]
        .bindHttp(config.serverConf.port, config.serverConf.host)
        .withHttpApp(httpApp)
        .resource
    } yield (server, checker)


  override def run(args: List[String]): IO[ExitCode] =
    createServer[IO].use { services =>
      IO.delay(services._1).foreverM.start *> services._2.runChecks.start *> IO.never
    }.as(ExitCode.Success)
}
