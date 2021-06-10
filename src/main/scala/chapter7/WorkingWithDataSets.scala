package chapter7

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

    // In Scala
    // Create a DataFrame with 10M records
    /**
      * The first count() materializes the cache, whereas the second one accesses the cache,
      * resulting in a close to 12 times faster access time for this data set.
      */
    val df = spark.range(1 * 10000000).toDF("id").withColumn("square", $"id" * $"id")
    df.cache() // Cache the data
    df.count() // Materialize the cache

    df.count() // Now get it from the cache
  }
}
