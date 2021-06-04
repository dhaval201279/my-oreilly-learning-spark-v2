package chapter6

import chapter6Domain.Usage
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{SparkSession, functions => F}
import org.apache.spark.sql.functions.expr

import scala.util.Random._

object WorkingWithDataSets {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .appName("WorkingWithDataSets")
      .getOrCreate();

    import spark.implicits._

    val r = new scala.util.Random(42)

    val data = for(i <- 0 to 1000)
      yield(Usage(i, "user-" + r.alphanumeric.take(5).mkString(""),
        r.nextInt(1000)))

    val dsUsage = spark
      .createDataset(data)
    dsUsage.show(10)

    // Filter
    dsUsage
      .filter(d => d.usage > 900)
      .orderBy(desc("usage"))
      .show(5, false)

    /** Another way for above filter e.g. is to define a function and supply that function as an argument to filter()
      * def filterWithUsage(u: Usage) = u.usage > 900
      * dsUsage.filter(filterWithUsage(_)).orderBy(desc("usage")).show(5)
      */

    // Use an if-then-else lambda expression and compute a value
    dsUsage
      .map(u => {if (u.usage > 750) u.usage * .15 else u.usage * .50 })
      .show(5, false)

    /** Another way of doing above if-then-else is to define a function and than use it
      *
      * 1. def computeCostUsage(usage: Int): Double = {
      *       if (usage > 750) usage * 0.15 else usage * 0.50
      *    }
      *
      * 2. dsUsage.map(u => {computeCostUsage(u.usage)}).show(5, false)
      *
      */
  }
}
