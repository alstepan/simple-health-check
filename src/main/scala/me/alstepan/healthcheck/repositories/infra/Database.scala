package me.alstepan.healthcheck.repositories.infra

import cats.implicits._
import cats.effect.{Async, Resource, Sync}
import doobie.hikari.HikariTransactor
import doobie.quill.DoobieContext
import io.getquill.{Escape, Literal, NamingStrategy}
import me.alstepan.healthcheck.config.DatabaseConfig
import org.flywaydb.core.Flyway

object Database {

  val doobieContext = new DoobieContext.Postgres(NamingStrategy(Escape, Literal))

  def makeTransactor[F[_]: Async](dbConf: DatabaseConfig): Resource[F, HikariTransactor[F]] = {
    for {
      ec <- Resource.eval(Async[F].executionContext)
      res <- HikariTransactor.newHikariTransactor(dbConf.driver, dbConf.url, dbConf.user, dbConf.password, ec)
    } yield res
  }

  def initialize[F[_]: Sync](dbConf: DatabaseConfig): F[Unit] =
    Sync[F].delay{
      val flyway = Flyway
        .configure()
        .dataSource(dbConf.url, dbConf.user, dbConf.password)
        .schemas("public")
        .defaultSchema("public")
        .load()
      flyway.repair()
      flyway.migrate()
    }.as(())
}
