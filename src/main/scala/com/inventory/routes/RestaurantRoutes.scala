package com.inventory.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, Route}
import spray.json.{DefaultJsonProtocol, JsObject, JsString, _}

import scala.concurrent.{ExecutionContext, Future}


object Models {
  //Probably would really use BigDecimal or Int rather than Double
  case class Item(id: String, desc: String, price: Double, numAvailable: Int)
}

import com.inventory.routes.Models._

trait Protocols extends DefaultJsonProtocol with SprayJsonSupport {
  //TODO Show what happens if you don't make this implicit
  implicit val itemFormat = jsonFormat4(Item.apply)

}

object TestOnly extends App with Protocols {
  println(
    Item(id = "hamburger", desc = "tasty hamburger", price = 8.00, numAvailable = 100).toJson)
}

trait RestaurantRoutes extends Protocols with Directives {
  implicit val executionContext: ExecutionContext

  //curl -v -X GET "http://localhost:7070/a/healthstatus"
  def requestHoldsNoValuesForUsReturnText: Route = {
    (get & path("a" / "healthstatus")) {

      complete(
        (StatusCodes.OK, "server healith is ok"))

    }
  }

  //curl -v -X GET "http://localhost:7070/a/text/hello"
  def returnPathValueAsText: Route = {
    (get & path("a" / "text" / Segment)) { pathVariable =>

      complete {
        Future.successful(
          (StatusCodes.OK, s"your path value is $pathVariable"))
      }

    }
  }

  //curl -v -X GET "http://localhost:7070/a/wrong/hello"
  var thePathValue: String = _
  def returnPathValueAsTextBad: Route = {
    (get & path("a" / "wrong" / Segment)) { pathVariable =>

      thePathValue = pathVariable
      complete {
        Future.successful(
          (StatusCodes.OK, s"your path value is $thePathValue"))
      }

    }
  }

  //curl -v -X GET "http://localhost:7070/a/b/hello" -H "accept: application/json"
  def simpleReturnJson: Route = {
    (get & path("a" / "b" / Segment)) { pathVariable =>


      complete {
        HttpResponse(StatusCodes.OK, entity = "hello there")
        Future.successful(
          (StatusCodes.OK, JsObject("yourPathValue" -> JsString(pathVariable))))
      }

    }
  }


  //TODO If we changed the order, would some routes not be accessible?
  def theRestaurantRoutes: Route = {
    requestHoldsNoValuesForUsReturnText ~
      returnPathValueAsText ~
      returnPathValueAsTextBad ~
      simpleReturnJson

  }

}

