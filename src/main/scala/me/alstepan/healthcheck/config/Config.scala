package me.alstepan.healthcheck.config

import scala.concurrent.duration.FiniteDuration

case class ServerConfig(host: String, port: Int)
case class HealthCheckConfig(scanFrequency: FiniteDuration, parallelism: Int)
case class AppConfig(serverConf: ServerConfig, checkerConf: HealthCheckConfig)
