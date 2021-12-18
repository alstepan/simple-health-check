package me.alstepan.healthcheck.simpleservice

import cats.effect.Async
import cats.effect.std.Random
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.DurationLong

class HealthService[F[_]: Async](config: Config, rnd: Random[F]) {

  object dsl extends Http4sDsl[F]
  import dsl._


  def health: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> root / "health" =>
      for {
        logger <- Slf4jLogger.create[F]
        num <- rnd.betweenInt(0, 100)
        delayNum <- rnd.betweenLong(config.minDelay.toMillis, config.maxDelay.toMillis)
        _ <- logger.info(s"Generated random number is $num with fail value ${config.failurePercentage}")
        _ <- logger.info(s"Expected service delay is $delayNum milliseconds. Delaying...")
        _ <- Async[F].sleep(delayNum.milliseconds)
        resp <-
          if (num < config.failurePercentage)
            InternalServerError(s"Simulating internal error because $num < ${config.failurePercentage}")
          else
            Ok("Health is ok")
        _ <- logger.info(s"Response is ready: $resp")
      } yield resp

  }
}

object HealthService {
  def apply[F[_]: Async](config: Config): F[HealthService[F]] =
    for {
      rnd <- Random.scalaUtilRandom[F]
    } yield new HealthService[F](config, rnd)
}
