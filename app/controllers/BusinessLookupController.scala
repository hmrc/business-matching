/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers

import connectors.EtmpConnector
import play.api.mvc._
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

object SaBusinessLookupController extends BusinessLookupController {
  val desConnector: EtmpConnector = EtmpConnector
}

object BusinessLookupController extends BusinessLookupController {
  val desConnector: EtmpConnector = EtmpConnector
}

object AgentBusinessLookupController extends BusinessLookupController {
  val desConnector: EtmpConnector = EtmpConnector
}


trait BusinessLookupController extends BaseController {

  def desConnector: EtmpConnector

  def lookup(id: String, utr: String, userType: String) = Action.async {
    implicit request =>
      val json = request.body.asJson.get
      desConnector.lookup(lookupData = json, userType = userType, utr = utr).map {
        lookupData =>
          lookupData.status match {
            case OK => Ok(lookupData.body)
            case NOT_FOUND => NotFound(lookupData.body).as("application/json")
            case BAD_REQUEST => BadRequest(lookupData.body)
            case SERVICE_UNAVAILABLE => ServiceUnavailable(lookupData.body)
            case INTERNAL_SERVER_ERROR | _ => InternalServerError(lookupData.body)
          }
      }
  }

}
