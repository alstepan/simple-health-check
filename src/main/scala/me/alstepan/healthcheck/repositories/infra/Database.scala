package me.alstepan.healthcheck.repositories.infra

import cats.implicits.*
import cats.effect.{Async, Resource, Sync}
import doobie.ExecutionContexts
import doobie.hikari.HikariTransactor
import me.alstepan.healthcheck.config.DatabaseConfig
import org.flywaydb.core.Flyway

object Database {

  def makeTransactor[F[_]: Async](dbConf: DatabaseConfig): Resource[F, HikariTransactor[F]] = {
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](4) // our connect EC    
      res <- HikariTransactor.newHikariTransactor(dbConf.driver, dbConf.url, dbConf.user, dbConf.password, ce)
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
