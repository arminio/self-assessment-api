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

import play.api.libs.json.{JsValue, Reads}
import play.api.mvc.{Action, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.selfassessmentapi.models.From
import uk.gov.hmrc.selfassessmentapi.resources.wrappers.{Response, ResponseHandler}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

trait Get[R <: Response] extends Actions with ResponseHandler[R] {
  type Args

  def get(nino: Nino, args: Args): Action[Unit] =
    APIAction(nino, sourceType, summary).async(parse.empty) { implicit request =>
      httpGet(nino, args) map (handle(_, responseMapper(nino, args)))
    }

  def httpGet(nino: Nino, args: Args)(implicit hc: HeaderCarrier): Future[R]

  def responseMapper(nino: Nino, args: Args): PartialFunction[R, Result]
}

object Get {
  type Aux[R <: Response, A] = Get[R] { type Args = A }
  def apply[R <: Response, A](implicit ev: Aux[R, A]): Aux[R, A] = implicitly
}

trait List[R <: Response] extends Actions with ResponseHandler[R] {
  def list(nino: Nino): Action[Unit] =
    APIAction(nino, sourceType, summary).async(parse.empty) { implicit request =>
      httpList(nino) map (handle(_, responseMapper(nino)))
    }

  def httpList(nino: Nino)(implicit hc: HeaderCarrier): Future[R]

  def responseMapper(nino: Nino): PartialFunction[R, Result]
}

object List {
  def apply[R <: Response](implicit l: List[R]): List[R] = implicitly
}

/**
  * Typeclass that represents a Play Action to POST a request to DES
  *
  * @tparam A API model
  * @tparam D DES model
  * @tparam R DES HTTP response wrapper
  */
trait Post[A, D, R <: Response] extends Actions with ResponseHandler[R] {
  type Args

  def post(nino: Nino, args: Args)(implicit reads: Reads[A], from: From[A, D]): Action[JsValue] =
    APIAction(nino, sourceType, summary).async(parse.json) { implicit request =>
      validate[A, R](request.body) { validatedRequest =>
        httpPost(nino, From[A, D].from(validatedRequest), args)
      } map {
        case Left(errorResult) => handleValidationErrors(errorResult)
        case Right(response) => handle(response, responseMapper(nino, args))
      }
    }

  def httpPost(nino: Nino, desRequest: D, args: Args)(implicit hc: HeaderCarrier): Future[R]

  def responseMapper(nino: Nino, args: Args): PartialFunction[R, Result]
}

object Post {
  type Aux[A, D, R <: Response, G] = Post[A, D, R] { type Args = G }
  def apply[A, D, R <: Response, G](implicit ev: Aux[A, D, R, G]): Aux[A, D, R, G] = implicitly
}

trait Put[A, D, R <: Response] extends Actions with ResponseHandler[R] {
  type Args

  def put(nino: Nino, args: Args)(implicit reads: Reads[A], from: From[A, D]): Action[JsValue] =
    APIAction(nino, sourceType, summary).async(parse.json) { implicit request =>
      validate[A, R](request.body) { validatedRequest =>
        httpPut(nino, From[A, D].from(validatedRequest), args)
      } map {
        case Left(errorResult) => handleValidationErrors(errorResult)
        case Right(response) => handle(response, responseMapper(nino, args))
      }
    }

  def httpPut(nino: Nino, desRequest: D, args: Args)(implicit hc: HeaderCarrier): Future[R]

  def responseMapper(nino: Nino, args: Args): PartialFunction[R, Result]
}

object Put {
  type Aux[A, D, R <: Response, G] = Put[A, D, R] { type Args = G }
  def apply[A, D, R <: Response, G](implicit ev: Aux[A, D, R, G]): Aux[A, D, R, G] = implicitly
}
