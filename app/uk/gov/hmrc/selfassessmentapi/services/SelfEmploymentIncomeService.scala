/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.selfassessmentapi.services

import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.selfassessmentapi.domain._

import scala.concurrent.Future

trait SelfEmploymentIncomeService {
  def find(saUtr: SaUtr): Future[Seq[SelfEmploymentIncome]]

  def create(selfEmploymentIncome: SelfEmploymentIncome): Future[SelfEmploymentIncomeId]

  def findBySelfEmploymentIncomeId(utr: SaUtr, selfEmploymentId: SelfEmploymentId, selfEmploymentIncomeId: SelfEmploymentIncomeId): Future[Option[SelfEmploymentIncome]]

  def update(selfEmploymentIncome: SelfEmploymentIncome, utr: SaUtr, selfEmploymentId: SelfEmploymentId, selfEmploymentIncomeId: SelfEmploymentIncomeId): Future[Unit]

  def delete(utr: SaUtr, selfEmploymentId: SelfEmploymentId, selfEmploymentIncomeId: SelfEmploymentIncomeId): Future[Boolean]
}
