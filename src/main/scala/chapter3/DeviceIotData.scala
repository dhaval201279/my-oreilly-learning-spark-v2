package chapter3

import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{SparkSession, functions => F}


import chapter3.{DeviceIoTDataDomain, DeviceTempByCountry}

object DeviceIotData {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .appName("DeviceIotData")
      .getOrCreate();

    import spark.implicits._

    // Read the file using the CSV DataFrameReader
    val iotDeviceJsonFile ="C:\\Users\\Dhaval\\Desktop\\temp\\iot_devices.json"
    val iotDataSet = spark
      .read
      .json(iotDeviceJsonFile)
      .as[DeviceIoTDataDomain]

    iotDataSet.show(5, false)

    // Flitering data set
    val filterTempDS = iotDataSet
      .filter({d => {d.temp > 30 && d.humidity > 70}})
    filterTempDS.show(5, false)

    // Filtering dataset into another smaller dataset ---> A
    val tempDS = iotDataSet
      .filter(d => {d.temp > 25})
      .map(d => (d.temp, d.device_name, d.device_id, d.cca3))
      .toDF("temp", "device_name", "device_id", "cca3")
      .as[DeviceTempByCountry]
    tempDS.show(5, false)

    // Alternative of above usage i.e. A
    val dsTemp2 = iotDataSet
      .select($"temp", $"device_name", $"device_id", $"device_id", $"cca3")
      .where("temp > 25")
      .as[DeviceTempByCountry]
    dsTemp2.show(5, false)

    // Only first row
    val device = tempDS.first()
    println(device)

    /**   EXERCISE
      *
      * Detect failing devices with battery levels below a threshold.
      *
      * Identify offending countries with high levels of CO2 emissions.
      *
      * Compute the min and max values for temperature, battery level, CO2, and humidity.
      *
      * Sort and group by average temperature, CO2, humidity, and country.
      *
      * */



  }
}
