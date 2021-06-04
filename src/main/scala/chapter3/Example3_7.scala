package chapter3

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._

object Example3_7 {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .appName("Example3_7")
      .getOrCreate();

    if (args.length <= 0) {
      println("usage Example3_7 file path missing");
      System.exit(1);
    }

    // Get the path to the JSON file
    val jsonFile = args(0);

    // Define our schema programmatically
    val schema = StructType(Array(StructField("Id", IntegerType, false),
      StructField("First", StringType, false),
      StructField("Last", StringType, false),
      StructField("Url", StringType, false),
      StructField("Published", StringType, false),
      StructField("Hits", IntegerType, false),
      StructField("Campaigns", ArrayType(StringType), false)))

    // Create a DataFrame by reading from the JSON file with a predefined schema
    val blogsDF = spark
      .read
      .schema(schema)
      .json(jsonFile)

    // Show the DataFrame schema as output
    blogsDF.show(false)

    // Print the schema
    println(blogsDF.printSchema())
    println(blogsDF.schema)

    // Printing columns from data frame
    println(s"Printing columns : $blogsDF.columns")

    // Access a particular column with col and it returns a Column type
    val colId = blogsDF.col("Id")
    println(s"Printing specific column : $colId")

    // Use an expression to compute a value
    val twoTimesHits = blogsDF
      .select(expr("Hits * 2"))
      .show(2)
    println(s"Printing computed value : $twoTimesHits")

    // or use col to compute value
    val colValueMultiplication = blogsDF
      .select(col("Hits") * 2)
      .show(2)
    println(s"Printing column ValueMultiplication : $colValueMultiplication")

    // Use an expression to compute big hitters for blogs
    // This adds a new boolean column, Big Hitters, based on the conditional expression
   val bitHittersNewColAddition = blogsDF
      .withColumn("Big Hitters", (expr("Hits > 10000")))
      .show()
    println(s"Printing bitHittersNewColAddition : $bitHittersNewColAddition")

    // Concatenate three columns, create a new column, and show the
    // newly created concatenated column
    val newColWithConcatenatedCols = blogsDF
      .withColumn("AuthorsId", (concat(expr("First"), expr("Last"), expr("Id"))))
      .select(col("AuthorsId"))
      .show(4)
    println(s"Printing newColWithConcatenatedCols : $newColWithConcatenatedCols")

    // These statements return the same value, showing that
    // expr is the same as a col method call
    blogsDF.select(expr("Hits")).show(2)
    blogsDF.select(col("Hits")).show(2)
    blogsDF.select("Hits").show(2)

    // Sort by column "Id" in descending order
    val sortByColId = blogsDF.sort(col("Id").desc).show()
    //blogsDF.sort($"Id".desc).show()
    println(s"Printing sortByColId : $sortByColId")
  }
}
