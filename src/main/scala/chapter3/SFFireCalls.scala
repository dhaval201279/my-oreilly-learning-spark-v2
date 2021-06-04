package chapter3

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{functions => F}

object SFFireCalls {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .appName("SFFireCalls")
      .getOrCreate();

    val fireSchema = StructType(Array(StructField("CallNumber", IntegerType, true),
      StructField("UnitID", StringType, true),
      StructField("IncidentNumber", IntegerType, true),
      StructField("CallType", StringType, true),
      StructField("CallDate", StringType, true),
      StructField("WatchDate", StringType, true),
      StructField("CallFinalDisposition", StringType, true),
      StructField("AvailableDtTm", StringType, true),
      StructField("Address", StringType, true),
      StructField("City", StringType, true),
      StructField("Zipcode", IntegerType, true),
      StructField("Battalion", StringType, true),
      StructField("StationArea", StringType, true),
      StructField("Box", StringType, true),
      StructField("OriginalPriority", StringType, true),
      StructField("Priority", StringType, true),
      StructField("FinalPriority", IntegerType, true),
      StructField("ALSUnit", BooleanType, true),
      StructField("CallTypeGroup", StringType, true),
      StructField("NumAlarms", IntegerType, true),
      StructField("UnitType", StringType, true),
      StructField("UnitSequenceInCallDispatch", IntegerType, true),
      StructField("FirePreventionDistrict", StringType, true),
      StructField("SupervisorDistrict", StringType, true),
      StructField("Neighborhood", StringType, true),
      StructField("Location", StringType, true),
      StructField("RowID", StringType, true),
      StructField("Delay", FloatType, true)))

    // Read the file using the CSV DataFrameReader
    val sfFireFile="data/sf-fire-calls.csv"
    val fireDF = spark
      .read
      .schema(fireSchema)
      .option("header", "true")
      .csv(sfFireFile)

    val fewFireDF = fireDF
      .select("IncidentNumber", "AvailableDtTm", "CallType")
      .where(col("CallType").notEqual("Medical Incident"))
    fewFireDF.show(5, false)

    //how many distinct CallTypes were recorded as the causes of the fire calls
    fireDF
      .select("CallType")
      .where(col("CallType").isNotNull)
      .agg(countDistinct("CallType") as "DistinctCallTypes")
      .show()

    // filter for only distinct non-null CallTypes from all the rows
    fireDF
      .select("CallType")
      .where(col("CallType").isNotNull)
      .distinct()
      .show(10, false)

    // selectively rename columns with the withColumnRenamed() method
    val newFireDF = fireDF.withColumnRenamed("Delay", "ResponseDelayedinMins")
    newFireDF
      .select("ResponseDelayedinMins")
      .where(col("ResponseDelayedinMins") > 5)
      .show(5, false)

    // Modifying contents of columns
    val fireTsDF = newFireDF
      .withColumn("IncidentDate", to_timestamp(col("CallDate"), "MM/dd/yyyy"))
      .drop("CallDate")
      .withColumn("OnWatchDate", to_timestamp(col("WatchDate"), "MM/dd/yyyy"))
      .drop("WatchDate")
      .withColumn("AvailableDtTS", to_timestamp(col("AvailableDtTm"),
        "MM/dd/yyyy hh:mm:ss a"))
      .drop("AvailableDtTm")
    fireTsDF
      .select("IncidentDate", "OnWatchDate", "AvailableDtTS")
      .show(5, false)

    // query new cols using functions from spark.sql.functions
    fireTsDF
      .select(year(col("IncidentDate")))
      .distinct()
      .orderBy(year(col("IncidentDate")))
      .show()

    /**
      * GROUPING AND AGGREGATING
      *
      * The DataFrame API also offers the collect() method, but for extremely large DataFrames this is resource-heavy (expensive)
      * and dangerous, as it can cause out-of-memory (OOM) exceptions. Unlike count(), which returns a single number to the driver,
      * collect() returns a collection of all the Row objects in the entire DataFrame or Dataset. If you want to take a peek at some
      * Row records you’re better off with take(n), which will return only the first n Row objects of the DataFrame.
      * */

    // most common types of fire calls
    fireTsDF
      .select("CallType")
      .where(col("CallType").isNotNull)
      .groupBy("CallType")
      .count()
      .orderBy(desc("count"))
      .show(10, false)

    // sum of alarms, the average response time, and the minimum and maximum response times to all fire calls in our data set
    fireTsDF
      .select(F.sum("NumAlarms"), F.avg("ResponseDelayedinMins"),
        F.min("ResponseDelayedinMins"), F.max("ResponseDelayedinMins"))
      .show()

    /**
      * End to End data frame e.g.
      *
      * What were all the different types of fire calls in 2018?
      *
      * What months within the year 2018 saw the highest number of fire calls?
      *
      * Which neighborhood in San Francisco generated the most fire calls in 2018?
      *
      * Which neighborhoods had the worst response times to fire calls in 2018?
      *
      * Which week in the year in 2018 had the most fire calls?
      *
      * Is there a correlation between neighborhood, zip code, and number of fire calls?
      *
      * How can we use Parquet files or SQL tables to store this data and read it back?
     */

    // What were all the different types of fire calls in 2018?
    /*fireDF
      .select("CallType")
      .where(col("CallType").isNotNull)



      .agg(countDistinct("CallType") as "DistinctCallTypes")
      .show()*/
  }
}
