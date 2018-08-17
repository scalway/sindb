package com.scalway.sindb

import com.scalway.sindb.DatabaseDef.UpgradeApi
import org.scalajs.dom
import org.scalajs.dom.idb.OpenDBRequest
import org.scalajs.dom.raw._
import org.scalajs.dom.window.indexedDB
import org.scalajs.dom.{ErrorEvent, Event}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.{JSON, |}
import scala.util.{Failure, Success, Try}
import utils.Implicits._

/**
  * Dibi api that does not typed. Just pure wrappers around IDB callbacks and similar stuff
  *
  */
abstract class DatabaseLowLevelApi(version:Int, name:String) {
  lazy val db:Future[IDBDatabase] = openDatabase()

  private def extractResultFromEvent(e:Event): Try[js.Any] = {
    lazy val res1 = e.target.!!.result
    lazy val err = e.target.!!.error.is[String]
    if (err != null) {
      Failure(new Exception(err))
    } else {
      Success(res1)
    }
  }

  private def request2Future(res: IDBRequest): Future[Event] = {
    val promise = Promise[Event]()

    res.onsuccess = (event:Event) => {
      promise.trySuccess(event)
    }

    res.onerror = (e:ErrorEvent) => {
      val msg = res.error
      dom.console.warn(JSON.stringify(msg))
      promise.tryFailure(new Exception(msg.toString))
    }

    promise.future
  }

  def withMultiTransaction[T](name:String | js.Array[String], allowWrite:Boolean = false)(op:IDBTransaction => Seq[IDBRequest]): Future[Seq[js.Any]] = db flatMap {
    database =>
      val req = database.transaction(name.asInstanceOf[js.Any], if (allowWrite) "readwrite" else "readonly")
      val events = op(req).map(request2Future)

      Future.sequence(
        events.map( eventF =>
          eventF.flatMap(event =>
            Future.fromTry(extractResultFromEvent(event))
          )
        )
      )
  }

  def withNewTransaction[T](name:String | js.Array[String], allowWrite:Boolean = false)(op:IDBTransaction => IDBRequest): Future[js.Any] = db flatMap {
    database =>
      val req = database.transaction(name.asInstanceOf[js.Any], if (allowWrite) "readwrite" else "readonly")
      request2Result(op(req))
  }

  def request2Result(req:IDBRequest): Future[js.Any] = {
    request2Future(req)
      .flatMap(s => Future.fromTry(extractResultFromEvent(s)))
  }

  /** it'll open new connection. I think it should be acceessed only internaly to create `val db`. **/
  private def openDatabase() = {
    val result = Promise[IDBDatabase]()
    val request = indexedDB.open(name, version)

    request.onsuccess = (evt:Event) => {
      result.success(
        evt.target.asInstanceOf[IDBOpenDBRequest].result.asInstanceOf[IDBDatabase]
      )
    }

    request.onerror = (evt:ErrorEvent) => {
      result.failure(new Exception(evt.message))
    }

    request.onupgradeneeded = (event:IDBVersionChangeEvent) => {
      dom.console.log(s"Sindb: onupgradeneeded ${event.oldVersion} -> ${event.newVersion}")
      val request = event.target.asInstanceOf[OpenDBRequest]
      val api = new UpgradeApi(request.result.asInstanceOf[IDBDatabase], request.transaction)
      upgrade(event.oldVersion, event.newVersion, api)
    }

    result.future
  }

  def upgrade(fromVersion:Int, toVersion:Int, db:UpgradeApi):Unit
}

