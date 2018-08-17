# SINDB (simple scala.js IndexedDB wrapper)

Simple helpers that simplifies usage of indexedDB in scalajs.

how to use it
------------------------------------------------------
You can use it in scalaJs and normal scala projects.
in build.sbt add:

```scala
resolvers += "scalway bintray repo" at "http://dl.bintray.com/scalway/maven"
libraryDependencies += "com.scalway" %%% "sindb" % "0.0.1"
```

Let assume that this is your's application model class:

```scala
import upickle.default._

case class TimeSpan(id:Int, area:Long, from:Long, to:Long)

object TimeSpan {
  implicit val RW: ReadWriter[TimeSpan] = macroRW[TimeSpan]
}
```

then in your code you can create and use database in such way:

```scala
import com.scalway.sindb.DatabaseDef

object db extends DatabaseDef(11, "dibitest") {
  val timespan = DBStore[TimeSpan, Int](1, "timespan", Text(_.id))
  val timearea = DBStore[TimeArea, Int](2, "timearea", Text(_.id))
}

db.timespan.getAll() //Future[Seq[TimeSpan]]
db.timespan.getAllBoun(lowerBound = 0)  //Future[Seq[TimeSpan]]
db.timespan.set(timespan) //Future[Unit]
db.timespan.get(1) //Future[TimeSpan]
db.timespan.delete(1) //Future[Unit]

```

Internals
---------

- We use [lihaoyi/sourcecode](https://github.com/lihaoyi/sourcecode) to gather path to id (rather risky way) in default implementation
- We use [lihaoyi/upickle](https://github.com/lihaoyi/upickle) to write scala case class objects directly to IndexedDB but you can replace this to different implementation
- implementation of serializers can be simply replaced by createing own DatabaseDef
- don't know if this project'll be supported in near future. I hope but it is not used on production right now. If it'll then sure.

Missing
-------

- indexes support
- better database evelution support/waiting for evelution scripts to finish
- more adwenced queries