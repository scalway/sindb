package com.scalway.sindb

import scala.util.Try

abstract class BiMap[T,E] {
  def r2l(t:T):E
  def l2r(e:E):T
}

object BiMap {
  def create[T,E](f:T => E, ff:E => T): BiMap[T, E] = new BiMap[T,E] {
    override def r2l(t: T): E = f(t)
    override def l2r(e: E): T = ff(e)
  }
  implicit val stringIntBiMap: BiMap[String, Int] = new BiMap[String, Int] {
    override def r2l(t: String): Int = t.trim.toInt
    override def l2r(e: Int): String = e.toString
  }

  implicit val stringDoubleBiMap: BiMap[String, Double] = new BiMap[String, Double] {
    override def r2l(t: String): Double = t.trim.toDouble
    override def l2r(e: Double): String = e.toString
  }

  implicit def stringSeqIntBiMap:BiMap[String, Seq[Int]] = new BiMap[String, Seq[Int]] {
    override def l2r(e: Seq[Int]): String = e.mkString(",")
    override def r2l(t: String): Seq[Int] = t.split(",").collect[Int, Seq[Int]] {
      case other if other.length > 0 => other.trim.toInt
    }
  }

  implicit def stringSeqDoubleBiMap:BiMap[String, Seq[Double]] = new BiMap[String, Seq[Double]] {
    override def l2r(e: Seq[Double]): String = e.mkString(",")
    override def r2l(t: String): Seq[Double] = t.split(",").collect[Double, Seq[Double]] {
      case other if other.length > 0 => other.trim.toDouble
    }
  }

  class EmptyStringOptBiMap[T,E](default:T, biMap:BiMap[T,E]) extends BiMap[T,Option[E]]{
    override def r2l(t: T): Option[E] = if (default != t) Try(biMap.r2l(t)).toOption else None
    override def l2r(e: Option[E]): T = e.map(s => biMap.l2r(s)).getOrElse(default)
  }

  implicit val stringOptIntBiMap: BiMap[String, Option[Int]] = new EmptyStringOptBiMap("", stringIntBiMap)
  implicit val stringOptDoubleBiMap:BiMap[String, Option[Double]] = new EmptyStringOptBiMap("", stringDoubleBiMap)

}