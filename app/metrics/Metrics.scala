/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package metrics

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer.Context
import metrics.MetricsEnum.MetricsEnum

class DefaultServiceMetrics extends ServiceMetrics
trait ServiceMetrics  {
  val registry: MetricRegistry = new MetricRegistry()

  val timers = Map(
    MetricsEnum.ETMP_BUSINESS_MATCH -> registry.timer("etmp-business-match-response-timer")
  )

  val successCounters = Map(
    MetricsEnum.ETMP_BUSINESS_MATCH -> registry.counter("etmp-business-match-success-counter")
  )

  val failedCounters = Map(
    MetricsEnum.ETMP_BUSINESS_MATCH -> registry.counter("etmp-business-match-failed-counter")
  )

  def startTimer(api: MetricsEnum): Context = timers(api).time()
  def incrementSuccessCounter(api: MetricsEnum): Unit = successCounters(api).inc()
  def incrementFailedCounter(api: MetricsEnum): Unit = failedCounters(api).inc()
}
