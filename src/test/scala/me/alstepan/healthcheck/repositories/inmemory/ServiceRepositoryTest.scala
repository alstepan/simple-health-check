package me.alstepan.healthcheck.repositories.inmemory

import cats.data.EitherT
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{Concurrent, IO}
import org.scalatest._
import cats.implicits._
import cats.effect.implicits._
import me.alstepan.healthcheck.Domain.Services._
import me.alstepan.healthcheck.repositories.ServiceRepository
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import java.net.URI
import scala.collection.immutable._
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps


class ServiceRepositoryTest extends AsyncFreeSpec with AsyncIOSpec with Matchers {

  def getRepo[F[_]: Concurrent] = EitherT.right[ServiceRepository.Error](ServiceRepository.inMemory[F])

  val srv1 = Service(ServiceId("123"), "service1", URI.create("http://localhost:1280/asd"), 10.seconds)
  val srv2 = Service(ServiceId("321"), "service2", URI.create("https://google:1280/health"), 50.seconds)

  "list" - {
    "should return empry list if no services were registered" in {
      getRepo[IO]
        .flatMap(r => EitherT.right[ServiceRepository.Error](r.list()))
        .foldF(err => fail(s"$err"), l => IO(l shouldBe empty))
    }
    "should always return all registered services" in {
      (for {
        repo <- getRepo[IO]
        _ <- repo.register(srv1)
        _ <- repo.register(srv2)
        list <- EitherT.right[ServiceRepository.Error](repo.list())
      } yield list).foldF(err => fail(s"Cannot list services: $err"), l => IO(l should contain only (srv1, srv2)))
    }
  }

  "service" - {
    "should return ServiceNotFound for uknown service" in {
      (for {
        repo <- getRepo[IO]
        _ <- repo.register(srv1)
        res <- repo.service(srv2.id)
      } yield res)
        .foldF(err => IO(err shouldBe a [ServiceRepository.ServiceNotFound]), s => fail(s"Service was registered: $s"))
    }
    "should return a service details for known service" in {
      (for {
        repo <- getRepo[IO]
        _ <- repo.register(srv1)
        _ <- repo.register(srv2)
        res <- repo.service(srv2.id)
      } yield res)
        .foldF(err => fail(s"$err"), s => IO(s shouldBe srv2))
    }
  }

  "register" - {
    "should add all new unique services" in {
      (for {
        repo <- getRepo[IO]
        res1 <- repo.register(srv1)
        res2 <- repo.register(srv2)
      } yield List(res1, res2)).foldF(err => fail(s"Cannot register service: $err"), r => IO(r should contain only ()))
    }

    "should return error if service have already been registered" in {
      (for {
        repo <- getRepo[IO]
        res1 <- repo.register(srv1)
        res2 <- repo.register(srv1)
      } yield List(res1, res2))
        .foldF[Assertion](
          err => IO(err shouldBe a [ServiceRepository.ServiceAlreadyRegistered]),
          l => fail(s"All services have been registered: $l"))
    }
  }

  "unregister" - {
    "should return ServiceNotFound error if service was not registered previously" in {
      (for {
        repo <- getRepo[IO]
        _ <- repo.register(srv1)
        res <- repo.unregister(srv2.id)
      } yield res)
        .foldF(err => IO(err shouldBe a [ServiceRepository.ServiceNotFound]), s => fail(s"Service was registered: $s"))
    }
    "should successfully remove service" in {
      (for {
        repo <- getRepo[IO]
        _ <- repo.register(srv1)
        _ <- repo.register(srv2)
        _ <- repo.unregister(srv1.id)
        res <- EitherT.right[ServiceRepository.Error](repo.list())
      } yield res)
        .foldF(err => fail(s"$err"), l => IO(l should contain only srv2))
    }
  }
}
