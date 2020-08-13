/*
 * Copyright 2020 HM Revenue & Customs
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
import javax.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton
class SaBusinessLookupController @Inject()(
                                            val desConnector: EtmpConnector,
                                            val cc: ControllerComponents
                                          ) extends BackendController(cc) with BusinessLookupController

@Singleton
class DefaultBusinessLookupController @Inject()(
                                                 val desConnector: EtmpConnector,
                                                 val cc: ControllerComponents
                                               ) extends BackendController(cc) with BusinessLookupController

@Singleton
class AgentBusinessLookupController @Inject()(
                                               val desConnector: EtmpConnector,
                                               val cc: ControllerComponents
                                             ) extends BackendController(cc) with BusinessLookupController

trait BusinessLookupController extends BackendController {
  val desConnector: EtmpConnector

  def lookup(id: String, utr: String, userType: String): Action[AnyContent] = Action.async {
    implicit request =>
      implicit val ec: ExecutionContext = controllerComponents.executionContext

      val json = request.body.asJson.get
      desConnector.lookup(lookupData = json, userType = userType, utr = utr) map {lookupData =>
        lookupData.status match {
          case OK                        => Ok(lookupData.body)
          case NOT_FOUND                 => NotFound(lookupData.body).as("application/json")
          case BAD_REQUEST               => BadRequest(lookupData.body)
          case SERVICE_UNAVAILABLE       => ServiceUnavailable(lookupData.body)
          case INTERNAL_SERVER_ERROR | _ => InternalServerError(lookupData.body)
        }
      }
  }

}
