package chapter5

import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{SparkSession, functions => F}
import org.apache.spark.sql.functions.expr

object CommonSparkSQLOperations {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .appName("CommonSparkSQLOperations")
      .getOrCreate();

    // Read the file using the CSV DataFrameReader
    // Set file paths
    val delaysPath =
    "H:\\Dhaval\\LearningSparkV2\\databricks-datasets\\learning-spark-v2\\flights\\departuredelays.csv"
    val airportsPath =
      "H:\\Dhaval\\LearningSparkV2\\databricks-datasets\\learning-spark-v2\\flights\\airport-codes-na.txt"

    val airports = spark
      .read
      .option("header", true)
      .option("inferschema", true)
      .option("delimeter", "\t")
      .csv(airportsPath)
    airports.createOrReplaceTempView("airports_na")

    val delays = spark
      .read
      .option("header", true)
      .csv(delaysPath)
      .withColumn("delay", expr("CAST(delay as INT) as delay"))
      .withColumn("distance", expr("CAST(distance as INT) as distance"))
    delays.createOrReplaceTempView("departuredelays")

    // Create temporary small table
    val foo = delays
      .filter(expr("""origin == 'SEA' AND destination == 'SFO' AND
      date like '01010%' AND delay > 0"""))
    foo.createOrReplaceTempView("foo")

    spark
      .sql("SELECT * FROM airports_na LIMIT 10")
      .show()

    spark
      .sql("SELECT * FROM departureDelays LIMIT 10")
      .show()

    spark
      .sql("SELECT * FROM foo")
      .show()

    // Union two tables
    val bar = delays.union(foo)
    bar.createOrReplaceTempView("bar")
    bar
      .filter(expr("""origin == 'SEA' AND destination == 'SFO'
        AND date LIKE '01010%' AND delay > 0"""))
      .show()

    // Inner join
    foo
      .join(airports
              .as("air"), col("air.IATA") === col("origin"))
      .select("City", "State", "date", "delay", "distance", "destination")
      .show()

    // Add new Column
    val foo2 = foo.withColumn(
      "status",
      expr("CASE WHEN delay <= 10 THEN 'On-time' ELSE 'Delayed' END")
    )

    // Drop columns
    val foo3 = foo2.drop("delay")
    foo3.show()

    // Renaming columns
    val foo4 = foo3.withColumnRenamed("status", "flight_status")
    foo4.show()
  }
}
