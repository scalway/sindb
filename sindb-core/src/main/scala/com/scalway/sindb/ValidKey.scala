package com.scalway.sindb

import scala.annotation.implicitNotFound
import scala.scalajs.js

/**
  * Type Class that puts a view bound on key types. Value types are not restricted much so I don't handle that
  */
@implicitNotFound("No implicit ValidKey defined for ${T}, thus it is not a valid Store Key type")
sealed trait ValidKey[T] {
  def toAny(a:T):js.Any = a.asInstanceOf[js.Any]
}

object ValidKey {
  implicit object StringOk extends ValidKey[String]
  implicit object IntOk extends ValidKey[Int]
  implicit object DoubleOk extends ValidKey[Double]
  implicit object IntSeqOk extends ValidKey[Seq[Int]]
  implicit object DoubleSeqOk extends ValidKey[Seq[Double]]
  implicit object IntArrayOk extends ValidKey[Array[Int]]
  implicit object DoubleArrayOk extends ValidKey[Array[Double]]
  implicit object StringSeqOk extends ValidKey[Seq[String]]
  implicit object StringArrayOk extends ValidKey[Array[String]]
  implicit object JsDateOk extends ValidKey[js.Date]
}