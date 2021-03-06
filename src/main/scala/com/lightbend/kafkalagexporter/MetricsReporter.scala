/*
 * Copyright (C) 2019 Lightbend Inc. <http://www.lightbend.com>
 */

package com.lightbend.kafkalagexporter

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{Behavior, PostStop}
import com.lightbend.kafkalagexporter.MetricsSink.{Message, Metric, Stop}

object MetricsReporter {
  def init(
    metricsSink: MetricsSink): Behavior[Message] = Behaviors.setup { _ =>
    reporter(metricsSink)
  }

  def reporter(metricsSink: MetricsSink): Behavior[Message] = Behaviors.receive {
    case (_, m: Metric) =>
      metricsSink.report(m)
      Behaviors.same
    case (context, _: Stop) =>
      Behaviors.stopped {
        Behaviors.receiveSignal {
          case (_, PostStop) =>
            metricsSink.stop()
            context.log.info("Gracefully stopped Prometheus metrics endpoint HTTP server")
            Behaviors.same
        }
      }
    case (context, m) =>
      context.log.error(s"Unhandled metric message: $m")
      Behaviors.same
  }
}
