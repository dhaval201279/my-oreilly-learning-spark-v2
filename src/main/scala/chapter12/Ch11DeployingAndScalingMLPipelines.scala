package chapter12

import chapter6Domain.Usage
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{SparkSession, functions => F}
import org.apache.spark.sql.functions.expr
import org.apache.spark.storage.StorageLevel

import org.apache.spark.ml.feature.VectorAssembler

import scala.util.Random._

object Ch11DeployingAndScalingMLPipelines {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .appName("Ch11DeployingAndScalingMLPipelines")
      .getOrCreate();

    import spark.implicits._

    // Configure source data path
    val sourcePath = "H:\\Dhaval\\Tech\\STS_Workspace\\oreilly-learning-sparkv2\\LearningSparkV2-master\\databricks-datasets\\learning-spark-v2\\sf-airbnb\\sf-airbnb-clean.parquet"


  }
}
