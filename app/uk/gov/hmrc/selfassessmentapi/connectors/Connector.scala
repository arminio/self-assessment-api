package uk.gov.hmrc.selfassessmentapi.connectors

import cats.data.Reader
import play.api.libs.json.Writes
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.selfassessmentapi.config.AppContext
import uk.gov.hmrc.selfassessmentapi.resources.wrappers.Response

import scala.concurrent.Future

trait Connector[R <: Response, V <: Verb] {
  type Args
  type ConfigFuture = Reader[Config[R, Args], Future[R]]

  lazy val baseUrl: String = AppContext.desUrl

  def config: Config[R, Args]

  def httpGet(nino: Nino, args: Args)(implicit hc: HeaderCarrier): ConfigFuture =
    Reader(config => config.httpConnector.httpGet[R](config.url(nino, args), config.toResponse))

  def httpPost[D](nino: Nino, desRequest: D, args: Args)(implicit w: Writes[D], hc: HeaderCarrier): ConfigFuture =
    Reader(config => config.httpConnector.httpPost[D, R](config.url(nino, args), desRequest, config.toResponse))

  def httpEmptyPost(nino: Nino, args: Args)(implicit hc: HeaderCarrier): ConfigFuture =
    Reader(config => config.httpConnector.httpEmptyPost[R](config.url(nino, args), config.toResponse))

  def httpPut[D](nino: Nino, desRequest: D, args: Args)(implicit w: Writes[D], hc: HeaderCarrier): ConfigFuture =
    Reader(config => config.httpConnector.httpPut[D, R](config.url(nino, args), desRequest, config.toResponse))
}

object Connector {
  type Aux[R <: Response, V <: Verb, A] = Connector[R, V] { type Args = A }
  def apply[R <: Response, V <: Verb, A](implicit ev: Aux[R, V, A]): Aux[R, V, A] = implicitly
}

sealed trait Verb

object Verb {
  case object Get extends Verb
  case object List extends Verb
  case object Post extends Verb
  case object EmptyPost extends Verb
  case object Put extends Verb
}

case class Config[R <: Response, A](url: (Nino, A) => String,
                                    toResponse: HttpResponse => R,
                                    httpConnector: HttpConnector)
