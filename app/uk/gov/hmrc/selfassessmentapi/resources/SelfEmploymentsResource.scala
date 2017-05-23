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

package uk.gov.hmrc.selfassessmentapi.resources

import play.api.Logger
import play.api.libs.json.{JsArray, JsValue, Json, Writes}
import play.api.mvc.Results._
import play.api.mvc.{Action, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.selfassessmentapi.connectors
import uk.gov.hmrc.selfassessmentapi.models.Errors.Error
import uk.gov.hmrc.selfassessmentapi.models.SourceType.SourceType
import uk.gov.hmrc.selfassessmentapi.models.des.Business
import uk.gov.hmrc.selfassessmentapi.models.selfemployment.{SelfEmployment, SelfEmploymentUpdate}
import uk.gov.hmrc.selfassessmentapi.models.{Errors, _}
import uk.gov.hmrc.selfassessmentapi.resources.wrappers.{EmptyBusinessData, EmptySelfEmployments, ParseError, SelfEmploymentResponse, _}

import scala.concurrent.Future

object SelfEmploymentsResource {
  val logger: Logger = Logger(this.getClass)

  implicit val get = new Get[SelfEmploymentResponse] {
    override type Args = SourceId

    override val sourceType: SourceType = SourceType.SelfEmployments

    override def httpGet(nino: Nino, args: Args)(implicit hc: HeaderCarrier): Future[SelfEmploymentResponse] =
      connectors.httpGet[SelfEmploymentResponse](baseUrl + s"/registration/business-details/nino/$nino",
                                                 SelfEmploymentResponse)

    override def responseMapper(nino: Nino, id: SourceId): PartialFunction[SelfEmploymentResponse, Result] = {
      case r @ Response(200) => handleRetrieve(r.selfEmployment(id), NotFound)
      case r @ Response(400) => BadRequest(Json.toJson(Errors.NinoInvalid))
      case Response(404) => NotFound
    }
  }

  implicit val list = new List[SelfEmploymentResponse] {
    override val sourceType: SourceType = SourceType.SelfEmployments

    override def httpList(nino: Nino)(implicit hc: HeaderCarrier): Future[SelfEmploymentResponse] =
      connectors.httpGet[SelfEmploymentResponse](baseUrl + s"/registration/business-details/nino/$nino",
                                                 SelfEmploymentResponse)

    override def responseMapper(nino: Nino): PartialFunction[SelfEmploymentResponse, Result] = {
      case r @ Response(200) => handleRetrieve(r.listSelfEmployment, Ok(JsArray()))
      case Response(400) => BadRequest(Json.toJson(Errors.NinoInvalid))
      case Response(404) => NotFound
    }
  }

  implicit val post = new Post[SelfEmployment, Business, SelfEmploymentResponse] {
    override val sourceType: SourceType = SourceType.SelfEmployments
    override type Args = NoArgs

    override def httpPost(nino: Nino, desRequest: Business, args: NoArgs)(
        implicit hc: HeaderCarrier): Future[SelfEmploymentResponse] =
      connectors.httpPost[Business, SelfEmploymentResponse](
        baseUrl + s"/income-tax-self-assessment/nino/$nino/business",
        desRequest,
        SelfEmploymentResponse)

    override def responseMapper(nino: Nino, args: Args): PartialFunction[SelfEmploymentResponse, Result] = {
      case r @ Response(200) => Created.withHeaders(LOCATION -> r.createLocationHeader(nino).getOrElse(""))
      case r @ (Response(400) | Response(409)) => BadRequest(Error.from(r.json))
      case Response(403) =>
        Forbidden(
          Json.toJson(
            Errors.businessError(
              Error(ErrorCode.TOO_MANY_SOURCES.toString,
                    s"The maximum number of Self-Employment incomes sources is 1",
                    Some("")))))
      case Response(404) => NotFound
    }
  }

  implicit val put = new Put[SelfEmploymentUpdate, des.SelfEmploymentUpdate, SelfEmploymentResponse] {
    override val sourceType: SourceType = SourceType.SelfEmployments
    override type Args = SourceId

    override def httpPut(nino: Nino, desRequest: des.SelfEmploymentUpdate, id: Args)(
        implicit hc: HeaderCarrier): Future[SelfEmploymentResponse] =
      connectors.httpPut[des.SelfEmploymentUpdate, SelfEmploymentResponse](
        baseUrl + s"/income-tax-self-assessment/nino/$nino/business/$id",
        desRequest,
        SelfEmploymentResponse)

    override def responseMapper(nino: Nino, args: Args): PartialFunction[SelfEmploymentResponse, Result] = {
      case Response(204) => NoContent
      case r @ Response(400) => BadRequest(Error.from(r.json))
      case Response(404) => NotFound
    }
  }

  def create(nino: Nino): Action[JsValue] =
    Post[SelfEmployment, Business, SelfEmploymentResponse, NoArgs].post(nino, ())

  // TODO: DES spec for this method is currently unavailable. This method should be updated once it is available.
  def update(nino: Nino, id: SourceId): Action[JsValue] =
    Put[SelfEmploymentUpdate, des.SelfEmploymentUpdate, SelfEmploymentResponse, SourceId].put(nino, id)

  def retrieve(nino: Nino, id: SourceId): Action[Unit] =
    Get[SelfEmploymentResponse, SourceId].get(nino, id)

  def retrieveAll(nino: Nino): Action[Unit] =
    List[SelfEmploymentResponse].list(nino)

  private def handleRetrieve[T](selfEmployments: Either[SelfEmploymentRetrieveError, T], resultOnEmptyData: Result)(
      implicit w: Writes[T]): Result =
    selfEmployments match {
      case error @ Left(EmptyBusinessData(_) | EmptySelfEmployments(_)) =>
        logger.warn(error.left.get.msg)
        resultOnEmptyData
      case Left(UnmatchedIncomeId(msg)) =>
        logger.warn(msg)
        NotFound
      case error @ Left(SelfEmploymentRetrieveError(msg)) =>
        error match {
          case Left(ParseError(_)) => logger.error(msg)
          case _ => logger.warn(msg)
        }
        InternalServerError(Json.toJson(Errors.InternalServerError))
      case Right(se) => Ok(Json.toJson(se))
    }
}
