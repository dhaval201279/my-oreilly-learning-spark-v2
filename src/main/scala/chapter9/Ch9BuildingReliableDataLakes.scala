package chapter9

import chapter6Domain.Usage
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{SparkSession, functions => F}
import org.apache.spark.sql.functions.expr
import org.apache.spark.storage.StorageLevel

import scala.util.Random._

object Ch9BuildingReliableDataLakes {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .appName("Ch9BuildingReliableDataLakes")
      .getOrCreate();

    import spark.implicits._

    // Configure source data path
    val sourcePath = "H:\\Dhaval\\Tech\\STS_Workspace\\oreilly-learning-sparkv2\\LearningSparkV2\\databricks-datasets\\learning-spark-v2\\loans\\loan-risks.snappy.parquet"

    // Configure Delta Lake path
    val deltaPath = "H:\\Dhaval\\Tech\\STS_Workspace\\oreilly-learning-sparkv2\\LearningSparkV2\\databricks-datasets\\learning-spark-v2\\loans\\tmp\\loans_delta"

    // Create the Delta table with the same loans data
    spark
      .read
      .format("parquet")
      .load(sourcePath)
      .write
      .format("delta")
      .save(deltaPath)

    // Create a view on the data called loans_delta
    spark
      .read
      .format("delta")
      .load(deltaPath)
      .createOrReplaceTempView("loans_delta")

    // Loans row count
    spark
      .sql("SELECT count(*) FROM loans_delta")
      .show()

    // First 5 rows of loans table
    spark
      .sql("SELECT * FROM loans_delta LIMIT 5")
      .show()

  }
}
