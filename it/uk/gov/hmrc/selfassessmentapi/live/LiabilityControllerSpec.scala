package uk.gov.hmrc.selfassessmentapi.live

import uk.gov.hmrc.support.BaseFunctionalSpec

class LiabilityControllerSpec extends BaseFunctionalSpec {

  val saUtr = generateSaUtr()

  "request liability" should {
    "return a 501 response" in {
      given().userIsAuthorisedForTheResource(saUtr)
        .when()
        .post(s"/$saUtr/liabilities")
        .thenAssertThat()
        .statusIs(501)
    }
  }

  "retrieve liability" should {
    "return a 501 response" in {
      given().userIsAuthorisedForTheResource(saUtr)
        .when()
        .get(s"/$saUtr/liabilities/1234")
        .thenAssertThat()
        .statusIs(501)
    }
  }

}
