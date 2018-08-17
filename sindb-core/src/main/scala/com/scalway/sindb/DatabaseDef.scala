package com.scalway.sindb

import com.scalway.sindb.DatabaseDef.{TR, UpgradeApi}
import org.scalajs.dom.raw._
import upickle.WebJson
import utils.Implicits._

import scala.annotation.implicitNotFound
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js


trait StoreApi[T, K] { orginal =>
  def delete(a:K)(implicit tr:TR = null):Future[Unit]
  def set(a:T)(implicit tr:TR = null):Future[T]
  def setAll(a:Seq[T])(implicit tr:TR = null):Future[Seq[T]]
  def clear()(implicit tr:TR = null):Future[Unit]
  def get(a:K)(implicit tr:TR = null):Future[T]
  def getAll()(implicit tr:TR = null):Future[Seq[T]]

  def biMap[T2, K2](implicit b:BiMap[T,T2], k:BiMap[K, K2]) =
    new MappedStoreApi[T, K, T2, K2](orginal, b, k)
}

trait DBStoreApi[T,K] extends StoreApi[T,K] {
  def upgrade(version:Int, db:UpgradeApi):Unit
}

class MappedStoreApi[T, K, T2, K2](orginal:StoreApi[T,K], b:BiMap[T,T2], k:BiMap[K, K2]) extends StoreApi[T2, K2] {
  override def delete(a: K2)(implicit tr: TR): Future[Unit] = orginal.delete(k.l2r(a))
  override def set(a: T2)(implicit tr:TR = null): Future[T2] = orginal.set(b.l2r(a)).map(_ => a)
  override def setAll(a: Seq[T2])(implicit tr:TR = null): Future[Seq[T2]] = orginal.setAll(a.map(b.l2r)).map(_ => a)
  override def clear()(implicit tr:TR = null): Future[Unit] = orginal.clear()
  override def get(a: K2)(implicit tr:TR = null): Future[T2] = orginal.get(k.l2r(a)).map(b.r2l)
  override def getAll()(implicit tr:TR = null): Future[Seq[T2]] = orginal.getAll().map(_.map(b.r2l))
}

abstract class DBStoreImpl[T:ValidValue, K:ValidKey](dibil:DatabaseLowLevelApi, storeName:String, key:sourcecode.Text[T => K]) extends DBStoreApi[T, K] {
  private val kbm = implicitly[ValidKey[K]]
  private val vbm = implicitly[ValidValue[T]]

  protected def maxUpdatesPerTransaction:Int = 50

  protected def request(allowWrite:Boolean = false, tr:TR = null)(os:IDBObjectStore => IDBRequest): Future[js.Any] = {
    if (tr == null) dibil.withNewTransaction(storeName, allowWrite)(ntr => os(ntr.objectStore(storeName)))
    else dibil.request2Result(os(tr.objectStore(storeName)))
  }


  override def delete(a: K)(implicit tr: TR = null): Future[Unit] = request(true, tr){ db =>
    db.delete(kbm.toAny(a))
  }.map(_ => ())

  def set(value:T)(implicit tr:TR = null): Future[T] = request(true, tr) { tr =>
    tr.put(vbm.obj2js(value, key))
  }.map(_ => value)

  def setAllInOneTransaction(a:Seq[T]):Future[Seq[T]] = {
    dibil.withMultiTransaction(storeName) { req =>
      a.map { l => req.objectStore(storeName).put(vbm.obj2js(l, key)) }
    }.map(_ => a)
  }

  def setAllJsArray(a:js.Array[js.Any])(implicit tr:TR = null):Future[Unit] = Future.sequence {
    a.map { l =>
      request(true, tr){
        tr => tr.put(l)
      }
    }.toSeq
  }.map(_ => ())

  def setAll(a:Seq[T])(implicit tr:TR = null):Future[Seq[T]] = Future.sequence {
    a.grouped(maxUpdatesPerTransaction).map(setAllInOneTransaction)
  }.map(_ => a)

  def clear()(implicit tr:TR = null):Future[Unit] = request(true, tr) {
    _.clear()
  }.map(_ => ())

  def getAllBound(lower:js.UndefOr[K] = js.undefined, upper:js.UndefOr[K] = js.undefined)(implicit proof:ValidKeyRange[K], tr:TR = null): Future[js.Array[T]] =
    request(false, tr) { os => os.!!.getAll(proof.create(lower, upper)).is[IDBRequest] }
      .mapTo[js.Array[js.Any]]
      .map(_.map(vbm.js2obj))

  def get(a:K)(implicit tr:TR = null):Future[T] = request(false, tr) {
    _.get(kbm.toAny(a))
  }.map(vbm.js2obj)

  def getAllJsArray()(implicit tr:TR = null):Future[js.Array[js.Any]] = request(false, tr) { os =>
    os.!!.getAll().is[IDBRequest]
  }.mapTo[js.Array[js.Any]]

  def getAll()(implicit tr:TR = null):Future[Seq[T]] = getAllJsArray()(tr).map(_.map(vbm.js2obj)) //.map(_.map(howToRead))
}

abstract class BasicDatabaseDef(version:Int, name:String) {
  protected val llapi: DatabaseLowLevelApi = new DatabaseLowLevelApi(version, name) {
    override def upgrade(fromVersion: Int, toVersion: Int, db: UpgradeApi): Unit = {
      (fromVersion.+(1) to toVersion).foreach { v =>
        allStores.foreach { s =>
          s.upgrade(v, db)
        }
      }
    }
  }

  import scalajs.js._

  def withTransaction[T](stores:DBStore[_, _] *)(tr:IDBTransaction => Seq[IDBRequest]): Future[Seq[Any]] = {
    llapi.withMultiTransaction(
      |.from(stores.map(_.storeName).to[js.Array]),
      allowWrite = true
    )(tr)
  }

  import DatabaseDef._

  private var allStores:Seq[DBStore[_,_]] = Nil

  protected case class DBStore[T:ValidValue, K:ValidKey](
      createVersion:Int, storeName:String, key:sourcecode.Text[T => K]
  ) extends DBStoreImpl[T, K](llapi, storeName, key) {
    val tvv: ValidValue[T] = implicitly[ValidValue[T]]
    private val ignoreFnc: Int => Unit = _ => ()

    allStores = allStores :+ this

    def upgrade(version:Int, api:UpgradeApi): Unit = {
      if (version == createVersion) tvv.createDatastore(api.db, storeName, key)
      onUpgrade(api).lift(version)
    }

    def onUpgrade(api:UpgradeApi):Upgrade = PartialFunction.empty
  }

  //protected def createObjectStore[T, K](store:DBStore[T, K], api:UpgradeApi)

  def isReady:Boolean = llapi.db.isCompleted

  def onReady():Future[this.type] = llapi.db.map {
    db => this
  }

}

@implicitNotFound("No implicit ValidValue defined for ${T}. You should have access to such inside definition of DatabaseDef {} class")
trait ValidValue[T] {
  def obj2js[K](a:T, key:sourcecode.Text[T => K]):js.Any
  def js2obj(a:js.Any):T
  def createDatastore[K](db:IDBDatabase, storeName:String, t:sourcecode.Text[T => K]):IDBObjectStore
}

/** this implementation writes objects directly without any wrappers using upickle.ReadWriter implementation. **/
class DatabaseDef(version:Int, name:String) extends BasicDatabaseDef(version, name) {
  import upickle.default._

  implicit protected def biJsAny[T:ReadWriter]: ValidValue[T] = new ValidValue[T] {
    val rw: ReadWriter[T] = implicitly[ReadWriter[T]]
    override def js2obj(t: js.Any): T = {
      if (!js.isUndefined(t)) WebJson.transform(t, rw)
      else throw new RuntimeException("Database: expected js.Any. got js.undefined")
    }
    override def obj2js[K](e: T, key:sourcecode.Text[T => K]): js.Any = rw.write(WebJson.Builder, e)

    override def createDatastore[K](db: IDBDatabase, storeName:String, key: sourcecode.Text[T => K]): IDBObjectStore = {
      db.createObjectStore(storeName, obj(keyPath = key.source.drop(2)))
    }
  }
}


/** this implementation writes objects to {k:key, v:SerializedObject} using upickle default. **/
class DatabaseDefStringBased(version:Int, name:String) extends BasicDatabaseDef(version, name) {
  import upickle.default._

  implicit protected def biJsAny[T:ReadWriter]: ValidValue[T] = new ValidValue[T] {
    val rw: ReadWriter[T] = implicitly[ReadWriter[T]]
    override def js2obj(t: js.Any): T = {
      if (!js.isUndefined(t)) read[T](t.!!.v.is[String])
      else throw new RuntimeException("Database: expected js.Any. got js.undefined")
    }
    override def obj2js[K](e: T, key:sourcecode.Text[T => K]): js.Any = obj(k = key.value(e).asInstanceOf[js.Any],v = write(e))

    override def createDatastore[K](db: IDBDatabase, storeName:String, key: sourcecode.Text[T => K]): IDBObjectStore = {
      db.createObjectStore(storeName, obj(keyPath = "k"))
    }
  }
}

object DatabaseDef {
  case class IDBUpgradeApi(db:IDBDatabase, transaction:TR)

  type TR = IDBTransaction
  type Upgrade = PartialFunction[Int, Unit]
  type UpgradeApi = IDBUpgradeApi
}