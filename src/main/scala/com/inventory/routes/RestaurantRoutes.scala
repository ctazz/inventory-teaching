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

  var inventory: Map[String, Item] = Map.empty

  //curl -v -X GET "http://localhost:7070/item/hamburger" -H "accept: application/json"
  def getItem: Route = {
    (get & path("item" / Segment)) { itemId =>
      complete(
        inventory.get(itemId) match {
          case Some(item) => StatusCodes.OK -> item.toJson //If itemFormat isn't implicit we need to do this:  item.toJson(itemFormat)
          case None => StatusCodes.NotFound -> JsObject("error" -> JsString(s"No item with id = $itemId"))
        })
    }
  }

  //TODO Could also do id=x&id=y
  //TODO Could also expect JSON Array of ids, although it's a bit unusual for a GET to contain JSON
  //curl -v -X GET "http://localhost:7070/item?ids=hamburger,pizza" -H "accept: application/json"
  def getItems: Route = {
    (get & path("item") & parameter("ids")) { commaDelimitedIds =>
      complete {
        val ids = commaDelimitedIds.split(",")
        ids.foldLeft((Vector.empty[String], Vector.empty[Item]))((badAndGood, id) => badAndGood match {
          case (failed, items) => inventory.get(id).map(item => (failed, items :+ item)).getOrElse((failed :+ id, items))
        }) match {
          case (bad, _) if !bad.isEmpty => StatusCodes.NotFound -> JsObject("error" -> JsString(s"Could not find these ids: ${bad.mkString(",")}"))
          case (_, items) => StatusCodes.OK -> items.toJson
        }

      }
    }
  }

  //curl -v -X GET "http://localhost:7070/items" -H "accept: application/json"
  def getAllItems: Route = {
    (get & path("items")) {
      complete {
        StatusCodes.OK -> inventory.values
/*        Future.successful(3).flatMap(_ =>
        Future.successful(
        StatusCodes.OK -> inventory.values
        ))*/
      }
    }
  }

  //curl -v -X POST "http://localhost:7070/item"  -H 'Content-Type: application/json'   -d  '{"desc":"tasty hamburger","id":"hamburger","numAvailable":100,"price":8.0}'
  def createItem: Route = {
    (post & path("item") & entity(as[Item])) { item =>
      complete {
        inventory = inventory + (item.id -> item)
        StatusCodes.Created
      }
    }
  }

  //curl -v -X PUT "http://localhost:7070/item"  -H 'Content-Type: application/json'   -d  '{"desc":"tasty hamburger","id":"hamburger","numAvailable":90,"price":9.0}'
  def modifyItem: Route = {
    (put & path("item") & entity(as[Item])) { item =>
      complete {

        inventory = inventory + (item.id -> item)
        StatusCodes.NoContent
      }
    }
  }

  //curl -v -X DELETE "http://localhost:7070/item/hamburger"
  def deleteItem: Route = {
    (delete & path("item" / Segment)) { itemId =>
      complete {
        inventory = inventory - itemId
        StatusCodes.NoContent
      }
    }
  }

  //TODO If we changed the order, would some routes not be accessible?
  def theRestaurantRoutes: Route = {
    requestHoldsNoValuesForUsReturnText ~
      returnPathValueAsText ~
      returnPathValueAsTextBad ~
      simpleReturnJson ~
      getItem ~
      getItems ~
      getAllItems ~
      createItem ~
      modifyItem ~
      deleteItem

  }

}

