/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.selfassessmentapi.connectors

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.selfassessmentapi.config.AppContext
import uk.gov.hmrc.selfassessmentapi.models.properties.PropertiesAnnualSummary
import uk.gov.hmrc.selfassessmentapi.models.properties.PropertyType.PropertyType
import uk.gov.hmrc.selfassessmentapi.models.{TaxYear, des, properties}
import uk.gov.hmrc.selfassessmentapi.resources.wrappers.PropertiesAnnualSummaryResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object PropertiesAnnualSummaryConnector {

  private lazy val baseUrl: String = AppContext.desUrl

  private def httpResponse2PropertiesResponse(propertyType: PropertyType, fut: Future[HttpResponse]): Future[PropertiesAnnualSummaryResponse] =
    fut.map(PropertiesAnnualSummaryResponse(propertyType, _))

  def update(nino: Nino, propertyType: PropertyType, taxYear: TaxYear, update: PropertiesAnnualSummary)(implicit hc: HeaderCarrier): Future[PropertiesAnnualSummaryResponse] = {
    val url: String = baseUrl + s"/income-store/nino/$nino/uk-properties/$propertyType/annual-summaries/${taxYear.toDesTaxYear}"
    update match {
      case other: properties.OtherPropertiesAnnualSummary => httpResponse2PropertiesResponse(propertyType, httpPut(url, des.OtherPropertiesAnnualSummary.from(other)))
      case fhl: properties.FHLPropertiesAnnualSummary => httpResponse2PropertiesResponse(propertyType, httpPut(url, des.FHLPropertiesAnnualSummary.from(fhl)))
    }
  }

  def get(nino: Nino, propertyType: PropertyType, taxYear: TaxYear)(implicit hc: HeaderCarrier): Future[PropertiesAnnualSummaryResponse] =
    httpResponse2PropertiesResponse(propertyType, httpGet(baseUrl + s"/income-store/nino/$nino/uk-properties/$propertyType/annual-summaries/${taxYear.toDesTaxYear}"))
}