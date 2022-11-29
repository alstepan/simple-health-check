package me.alstepan.healthcheck.config

import zio.*
import zio.config.*, ConfigDescriptor.*
import zio.config.typesafe.*
import zio.config.syntax.*
import zio.config.magnolia.*

val hcConfigDesc = descriptor[HealthCheckConfig]
val appConfigDesc = descriptor[AppConfig]
