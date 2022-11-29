package me.alstepan.healthcheck

import cats.effect.*
import cats.implicits.*
import doobie.Transactor
import me.alstepan.healthcheck.API.{ServiceRegistry, Statistics}
import me.alstepan.healthcheck.config.{AppConfig, DatabaseConfig, HealthCheckConfig, ServerConfig}
import me.alstepan.healthcheck.repositories.infra.Database
import me.alstepan.healthcheck.repositories.{HealthCheckRepository, ServiceRepository}
import me.alstepan.healthcheck.services.HealthChecker
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.middleware.*
import org.http4s.server.staticcontent.*
import org.http4s.server.{Router, Server}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import io.circe.*
import io.circe.generic.auto.*
import io.circe.yaml
import  me.alstepan.healthcheck.API.JsonFormats.given

import scala.concurrent.duration.DurationInt
import scala.io.Source
import com.comcast.ip4s.*

object Server extends IOApp {

  val defaultConfig: AppConfig = AppConfig(
    serverConf = ServerConfig("0.0.0.0", 8080),
    checkerConf = HealthCheckConfig(5.seconds, 5),
    None
  )

  val cors = CORS.policy.withAllowOriginAll

  case class Environment[F[_]](serviceRepo: ServiceRepository[F], healchCheckRepo: HealthCheckRepository[F])

  def createEnvironment[F[_]: Async](dbConf: Option[DatabaseConfig]): Resource[F, Environment[F]] =
    dbConf.fold(Resource.eval(memoryEnvironment)){c =>
      for {
        _ <- Resource.eval(Database.initialize(c))
        t <- Database.makeTransactor(c)
        e <- Resource.eval(dbEnvironment(t))
      } yield e
    }

  def dbEnvironment[F[_]: Sync](tr: Transactor[F]): F[Environment[F]] =
    for {
      logger <- Slf4jLogger.create[F]
      _ <- logger.info("Creating database repositories")
      sr <- ServiceRepository.doobie(tr)
      hcr <- HealthCheckRepository.doobie(tr)
    } yield Environment(sr, hcr)

  def memoryEnvironment[F[_]: Async]: F[Environment[F]] =
    for {
      logger <- Slf4jLogger.create[F]
      _ <- logger.info("Creating In-memory repositories")
      sr <- ServiceRepository.inMemory[F]
      hcr <- HealthCheckRepository.inMemory[F]
    } yield Environment(sr, hcr)

  def createServer[F[_]: Async]: Resource[F, (Server, HealthChecker[F])] =
    for {
      logger <- Resource.eval(Slf4jLogger.create[F])
      config <- Resource.eval(
        Async[F].delay(
            yaml
              .parser
              .parse(Source.fromResource("application.yaml").reader())
              .flatMap(_.as[AppConfig])
              .getOrElse(defaultConfig)     
        )
      )

      environment <- createEnvironment(config.dbConf)

      httpApp = cors.apply(
        Router(
          "registry" -> ServiceRegistry.endpoints[F](environment.serviceRepo),
          "stat" -> Statistics.endpoints[F](environment.healchCheckRepo),
          "monitor" -> resourceServiceBuilder[F]("/assets").toRoutes
        ).orNotFound
      )
      client = EmberClientBuilder.default[F].build
      checker <- Resource.eval(
        HealthChecker.apply[F](client, environment.serviceRepo, environment.healchCheckRepo, config.checkerConf))
      server <- EmberServerBuilder.default[F]
        .withHost(Host.fromString(config.serverConf.host).getOrElse(host"localhost"))
        .withPort(Port.fromInt(config.serverConf.port).getOrElse(port"8080"))
        .withHttpApp(httpApp)
        .withLogger(logger)
        .build
    } yield (server, checker)


  override def run(args: List[String]): IO[ExitCode] =
    createServer[IO].use { services =>
      IO.delay(services._1).foreverM.start *> services._2.runChecks.start *> IO.never
    }.as(ExitCode.Success)
}
