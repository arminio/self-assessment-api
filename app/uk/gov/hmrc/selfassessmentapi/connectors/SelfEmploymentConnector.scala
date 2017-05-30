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
import uk.gov.hmrc.selfassessmentapi.models.SourceId
import uk.gov.hmrc.selfassessmentapi.resources.wrappers.SelfEmploymentResponse

object SelfEmploymentConnector {

  lazy val httpConnector: HttpConnector = HttpConnector()

  implicit val getConn = new Connector[SelfEmploymentResponse, Verb.Get.type] { self =>
    override type Args = SourceId

    def mkUrl(nino: Nino, args: Args): String = baseUrl + s"/registration/business-details/nino/$nino"

    override def config = Config(url = mkUrl, toResponse = SelfEmploymentResponse, httpConnector = httpConnector)
  }

  implicit val listConn = new Connector[SelfEmploymentResponse, Verb.List.type] { self =>
    override type Args = Unit

    def mkUrl(nino: Nino, args: Args): String = baseUrl + s"/registration/business-details/nino/$nino"

    override def config = Config(url = mkUrl, toResponse = SelfEmploymentResponse, httpConnector = httpConnector)
  }

  implicit val postConn = new Connector[SelfEmploymentResponse, Verb.Post.type] { self =>
    override type Args = Unit

    def mkUrl(nino: Nino, args: Args): String = baseUrl + s"/income-tax-self-assessment/nino/$nino/business"

    override def config = Config(url = mkUrl, toResponse = SelfEmploymentResponse, httpConnector = httpConnector)
  }

  implicit val putConn = new Connector[SelfEmploymentResponse, Verb.Put.type] { self =>
    override type Args = SourceId

    def mkUrl(nino: Nino, args: Args): String = baseUrl + s"/income-tax-self-assessment/nino/$nino/business/$args"

    override def config = Config(url = mkUrl, toResponse = SelfEmploymentResponse, httpConnector = httpConnector)
  }
}