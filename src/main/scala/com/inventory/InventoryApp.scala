package com.inventory

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.inventory.routes.RestaurantRoutes

import scala.util.Failure
import scala.util.Success

//#main-class
object InventoryApp extends App with RestaurantRoutes{

  val rootBehavior = Behaviors.setup[Nothing] { context =>

    //val routes = new UserRoutes(userRegistryActor)(context.system)
    startHttpServer(theRestaurantRoutes, context.system)

    Behaviors.empty
  }
  val system = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")

  implicit val classicSystem: akka.actor.ActorSystem = system.toClassic
  implicit val executionContext = system.executionContext

  //#start-http-server
  private def startHttpServer(routes: Route, system: ActorSystem[_]): Unit = {


    //TODO The port should come from config
    val futureBinding = Http().bindAndHandle(routes, "localhost", 7070)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

}
//#main-class
