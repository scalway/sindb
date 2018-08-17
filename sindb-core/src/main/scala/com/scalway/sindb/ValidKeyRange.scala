package com.scalway.sindb

import org.scalajs.dom.raw.IDBKeyRange

import scala.scalajs.js

class ValidKeyRange[K:ValidKey] {
  val validKey: ValidKey[K] = implicitly[ValidKey[K]]

  def create(lowerBound:js.UndefOr[K] = js.undefined, upperBound:js.UndefOr[K] = js.undefined): js.UndefOr[IDBKeyRange] = {
    val lb = lowerBound.map(validKey.toAny)
    val ub = upperBound.map(validKey.toAny)
    (lb, ub) match {
      case _ if lb.isEmpty && ub.isEmpty => js.undefined
      case _ if lb.isEmpty => IDBKeyRange.upperBound(ub)
      case _ if ub.isEmpty => IDBKeyRange.lowerBound(lb)
      case _ => IDBKeyRange.bound(lb, ub)
    }
  }
}

object ValidKeyRange {
  implicit val StringOk: ValidKeyRange[String] = new ValidKeyRange[String]
  implicit val IntOk: ValidKeyRange[Int] = new ValidKeyRange[Int]
  implicit val DoubleOk: ValidKeyRange[Double] = new ValidKeyRange[Double]
}

