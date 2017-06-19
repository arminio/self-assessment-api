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

package uk.gov.hmrc.selfassessmentapi.resources.wrappers

import uk.gov.hmrc.play.http.HttpResponse
import uk.gov.hmrc.selfassessmentapi.models.calculation.TaxCalculation
import uk.gov.hmrc.selfassessmentapi.models.des
import uk.gov.hmrc.selfassessmentapi.models.des.{DesError, DesErrorCode}

case class TaxCalculationResponse(underlying: HttpResponse) extends Response {
  def calcId: Option[String] = {
    (json \ "id").asOpt[String] match {
      case x @ Some(_) => x
      case None => {
        logger.error("The 'id' field was not found in the response from DES")
        None
      }
    }
  }

  def calculation: Option[TaxCalculation] = {
    (json \ "calcResult" \ "calcDetail").asOpt[des.TaxCalculation] match {
      case x @ Some(_) => updateIncomeTax(x)
      case None => {
        logger.error("The 'calcDetail' field was not found in the response from DES")
        None
      }
    }
  }

  def updateIncomeTax(calc: Option[TaxCalculation]): Option[TaxCalculation] = {
    val incomeTaxYTD = (json \ "calcResult" \ "incomeTaxYTD").as[BigDecimal]
    val incomeTaxThisPeriod = (json \ "calcResult" \ "incomeTaxYTD").as[BigDecimal]
    Some(calc.get.copy(incomeTaxYTD = Some(incomeTaxYTD), incomeTaxThisPeriod = Some(incomeTaxThisPeriod)))
  }

  def isInvalidCalcId: Boolean =
    json.asOpt[DesError].exists(_.code == DesErrorCode.INVALID_CALCID)

  def isInvalidNino: Boolean =
    json.asOpt[DesError].exists(_.code == DesErrorCode.INVALID_NINO)

  def isInvalidIdentifier: Boolean =
    json.asOpt[DesError].exists(_.code == DesErrorCode.INVALID_IDENTIFIER)
}
