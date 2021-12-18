package me.alstepan.healthcheck.simpleservice

import scala.concurrent.duration.{DurationInt, FiniteDuration}

case class Config(port: Int = 8086,
                  host: String = "0.0.0.0",
                  failurePercentage: Int = 50,
                  minDelay: FiniteDuration = 5.milliseconds,
                  maxDelay: FiniteDuration = 10.milliseconds
                 )