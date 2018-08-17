package com.scalway.sindb.utils

import scala.language.dynamics
import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.{Dynamic, UndefOr}

class AsDynamic[T <: js.Any](val a:T) extends AnyVal {
  @inline def !! : Dynamic = a.asInstanceOf[js.Dynamic]
}

class DynamicToAnything[T <: js.Dynamic](val a:T) extends AnyVal {
  def is[ANYTHING]: ANYTHING = a.asInstanceOf[ANYTHING]
  def isUndefOr[ANYTHING]: UndefOr[ANYTHING] = a.asInstanceOf[UndefOr[ANYTHING]]
  def orIfUndef[ANYTHING](a:ANYTHING): ANYTHING = a.asInstanceOf[UndefOr[ANYTHING]].getOrElse(a)
  def isUndef: Boolean =  js.typeOf(a) == "undefined"
}

trait BasicImplicits {
  /** shortcut to create javascript literals */
  val G: Dynamic = Dynamic.global
  val obj: Dynamic.literal.type = Dynamic.literal

  implicit def asDynamic[T <: js.Any](a:T):AsDynamic[T] = new AsDynamic(a)
  implicit def dynamicToAnything[T <: js.Dynamic](a:T):DynamicToAnything[T] = new DynamicToAnything(a)
}

object Implicits extends BasicImplicits
