package me.alstepan.healthcheck.simpleservice

import cats.data.EitherT
import cats.effect.{ExitCode, IO, IOApp, Resource}
import org.http4s.blaze.server.BlazeServerBuilder

import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt

object Main extends IOApp {

  @tailrec
  def parseArgs(args: List[String], acc: Config): Either[String, Config] = args match {
    case Nil => Right(acc)
    case "--port" :: arg :: tail => parseArgs(tail, acc.copy(port = arg.toInt))
    case "--fails" :: arg :: tail => parseArgs(tail, acc.copy(failurePercentage = arg.toInt))
    case "--mindelay" :: arg :: tail => parseArgs(tail, acc.copy(minDelay = arg.toInt.milliseconds))
    case "--maxdelay" :: arg :: tail => parseArgs(tail, acc.copy(maxDelay = arg.toInt.milliseconds))
    case "--host" :: arg :: tail => parseArgs(tail, acc.copy(host = arg))
    case arg :: _ => Left(arg)
  }

  def usage(arg: String): IO[Unit] = IO(
    println(s"""Unknown argument $arg.
                |Usage: java -jar simpleservice.jar [--port <port> --fails  <num fails> --mindelay <ms> --maxdelay <ms>]
                |where num fails is a number from 0 to 100
                |ms - time of delay in milliseconds
                |""".stripMargin)
  )

  def createServer(config: Config): Resource[IO, Unit] =
    for {
      service <- Resource.eval(HealthService[IO](config))
      _ <- BlazeServerBuilder[IO]
       .bindHttp(config.port, config.host)
       .withHttpApp(service.health.orNotFound)
       .resource
    } yield ()

  override def run(args: List[String]): IO[ExitCode] =
    EitherT.fromEither[IO](parseArgs(args, Config())).leftMap(arg => usage(arg))
      .foldF(
        err => err.as(ExitCode.Error),
        config => createServer(config).use(_ => IO.never).as(ExitCode.Success)
      )

}
