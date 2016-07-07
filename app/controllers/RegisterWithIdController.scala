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

package controllers

import connectors.RegisterWithIdConnector
import play.api.mvc.Action
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait RegisterWithIdController extends BaseController {

  def connector: RegisterWithIdConnector

  def lookup(gg: String) = Action.async {
    implicit request =>

      request.body.asJson match {
        case Some(x) =>
          connector.lookup(x) map {
            lookupData =>
              lookupData.status match {
                case OK => Ok (lookupData.body)
                case NOT_FOUND => NotFound (lookupData.body).as ("application/json")
                case BAD_REQUEST => BadRequest (lookupData.body)
                case SERVICE_UNAVAILABLE => ServiceUnavailable (lookupData.body)
                case INTERNAL_SERVER_ERROR | _ => InternalServerError (lookupData.body)
              }
          }
        case None =>
          Future.successful(BadRequest)
      }
  }
}

object RegisterWithIdController extends RegisterWithIdController {
  val connector: RegisterWithIdConnector = RegisterWithIdConnector
}
