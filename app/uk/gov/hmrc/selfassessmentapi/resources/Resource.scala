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

import cats.data.Reader
import play.api.libs.json.{JsValue, Reads, Writes}
import play.api.mvc.{Action, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.selfassessmentapi.connectors.{Connector, Verb}
import uk.gov.hmrc.selfassessmentapi.models.Transform
import uk.gov.hmrc.selfassessmentapi.resources.wrappers.{Response, ResponseHandler}

import scala.concurrent.ExecutionContext.Implicits._

object Resource {
  trait Read[R <: Response, V <: Verb] extends Actions2 with ResponseHandler[R] {
    type Args

    def httpRead(
        nino: Nino,
        args: Args,
        responseMapper: (Nino, Args) => PartialFunction[R, Result]): Reader[Config[R, V, Args], Action[Unit]] =
      Reader(config =>
        APIAction(nino, sourceType, summary).run(config.actionConfig).async(parse.empty) { implicit request =>
          config.connector
            .httpGet(nino, args) map (_ map (handle(_, responseMapper(nino, args)))) run config.connector.config
      })
  }

  trait Write[R <: Response, V <: Verb, A, D] extends Actions2 with ResponseHandler[R] {
    type Args

    def httpWrite(v: V, nino: Nino, args: Args, responseMapper: (Nino, Args) => PartialFunction[R, Result])(
        implicit Reads: Reads[A],
        Writes: Writes[D],
        Transform: Transform[A, D]): Reader[Config[R, V, Args], Action[JsValue]] =
      Reader(config =>
        APIAction(nino, sourceType, summary).run(config.actionConfig).async(parse.json) { implicit request =>
          validate[A, R](request.body) { validatedRequest =>
            v match {
              case _: Verb.Post.type =>
                config.connector.httpPost(nino, Transform.from(validatedRequest), args) run config.connector.config
              case _: Verb.Put.type =>
                config.connector.httpPut(nino, Transform.from(validatedRequest), args) run config.connector.config
            }
          } map {
            case Left(errorResult) => handleValidationErrors(errorResult)
            case Right(response) => handle(response, responseMapper(nino, args))
          }
      })
  }

  trait Get[R <: Response] extends Read[R, Verb.Get.type] {
    type Args

    def get(nino: Nino, args: Args): Reader[Config[R, Verb.Get.type, Args], Action[Unit]] =
      httpRead(nino, args, responseMapper)

    def responseMapper(nino: Nino, args: Args): PartialFunction[R, Result]
  }

  object Get {
    type Aux[R <: Response, A] = Get[R] { type Args = A }

    def apply[R <: Response, A](implicit ev: Aux[R, A]): Aux[R, A] = implicitly
  }

  trait List[R <: Response] extends Read[R, Verb.List.type] {
    type Args = Unit
    def list(nino: Nino): Reader[Config[R, Verb.List.type, Args], Action[Unit]] =
      httpRead(nino, (), responseMapper)

    def responseMapper(nino: Nino, args: Unit): PartialFunction[R, Result]
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
  trait Post[A, D, R <: Response] extends Write[R, Verb.Post.type, A, D] {
    type Args

    def post(nino: Nino, args: Args)(
        implicit Reads: Reads[A],
        Writes: Writes[D],
        Transform: Transform[A, D]): Reader[Config[R, Verb.Post.type, Args], Action[JsValue]] =
      httpWrite(Verb.Post, nino, args, responseMapper)

    def responseMapper(nino: Nino, args: Args): PartialFunction[R, Result]
  }

  object Post {
    type Aux[A, D, R <: Response, G] = Post[A, D, R] { type Args = G }

    def apply[A, D, R <: Response, G](implicit ev: Aux[A, D, R, G]): Aux[A, D, R, G] = implicitly
  }

  trait Put[A, D, R <: Response] extends Write[R, Verb.Put.type, A, D] {
    type Args

    def put(nino: Nino, args: Args)(
        implicit Reads: Reads[A],
        Writes: Writes[D],
        Transform: Transform[A, D]): Reader[Config[R, Verb.Put.type, Args], Action[JsValue]] =
      httpWrite(Verb.Put, nino, args, responseMapper)

    def responseMapper(nino: Nino, args: Args): PartialFunction[R, Result]
  }

  object Put {
    type Aux[A, D, R <: Response, G] = Put[A, D, R] { type Args = G }

    def apply[A, D, R <: Response, G](implicit ev: Aux[A, D, R, G]): Aux[A, D, R, G] = implicitly
  }

  case class Config[R <: Response, V <: Verb, A](actionConfig: ActionConfig, connector: Connector.Aux[R, V, A])
}
