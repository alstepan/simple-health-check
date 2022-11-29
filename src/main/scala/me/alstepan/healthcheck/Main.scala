package me.alstepan.healthcheck

import zio.*
import zio.Console.*
import zhttp.http.*
import zio.config.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import zhttp.service.Server
import me.alstepan.healthcheck.API.ServiceRegistry
import me.alstepan.healthcheck.config.{AppConfig, appConfigDesc}
import me.alstepan.healthcheck.repositories.inmemory.ServiceRepositoryImpl as MemoryRepo
import me.alstepan.healthcheck.repositories.db.ServiceRepositoryImpl as DBRepo
import zio.config.typesafe.TypesafeConfig

object Main extends ZIOAppDefault:

  val myapp: ZIO[Any, Throwable, zio.ExitCode] =
    ZIO.scoped {
      DBRepo.layer.memoize.flatMap{ repo =>
        (for {
          appConfig <- getConfig[AppConfig]
          _ <- Server.start(appConfig.serverConf.port,
            ServiceRegistry
              .all
              .provideSomeLayer(repo)
          )
        } yield ExitCode.success).provide(
          Quill.Postgres.fromNamingStrategy(LowerCase),
          Quill.DataSource.fromPrefix("database"),
          TypesafeConfig.fromResourcePath(appConfigDesc)
        )
      }
    }

  override def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] = myapp
