package chapter10

import chapter6Domain.Usage
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{SparkSession, functions => F}
import org.apache.spark.sql.functions.expr
import org.apache.spark.storage.StorageLevel

import org.apache.spark.ml.feature.VectorAssembler

import scala.util.Random._

object Ch10MachineLearningWithMLLib {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .appName("Ch10MachineLearningWithMLLib")
      .getOrCreate();

    import spark.implicits._

    // Configure source data path
    val sourcePath = "H:\\Dhaval\\Tech\\STS_Workspace\\oreilly-learning-sparkv2\\LearningSparkV2-master\\databricks-datasets\\learning-spark-v2\\sf-airbnb\\sf-airbnb-clean.parquet"

    val airbnbDF = spark
                    .read
                    .parquet(sourcePath)

    airbnbDF
      .select("neighbourhood_cleansed", "room_type", "bedrooms", "bathrooms",
        "number_of_reviews", "price")
      .show(5)

    // Split entire data set into training set (80 %)and test set (20 %)
    val Array(trainDF, testDF) = airbnbDF.randomSplit(Array(.8, .2), seed=42)
    println(f"""There are ${trainDF.count} rows in the training set, and ${testDF.count} in the test set""")

    /**
      * After splitting entire data into training and test set, lets build a linear regression model
      * predicting price given the number of bedrooms. Linear regression (like many other algorithms in Spark) requires
      * that all the input features are contained within a single vector in your DataFrame. Thus, we need to transform our data.
      *
      * VectorAssembler takes a list of input columns and creates a new DataFrame with an additional column,
      * which we will call features. It combines the values of those input columns into a single vector
      */
    val vecAssembler = new VectorAssembler()
      .setInputCols(Array("bedrooms"))
      .setOutputCol("features")
    val vecTrainDF = vecAssembler.transform(trainDF)
    vecTrainDF.select("bedrooms", "features", "price").show(10)


    /**
      * In Spark, LinearRegression is a type of estimator—it takes in a DataFrame and returns a Model.
      * Estimators learn parameters from your data, have an estimator_name.fit() method, and are eagerly evaluated
      * (i.e., kick off Spark jobs), whereas transformers are lazily evaluated. Some other examples of estimators include
      * Imputer, DecisionTreeClassifier, and RandomForestRegressor
      */

    import org.apache.spark.ml.regression.LinearRegression
    val lr = new LinearRegression()
      .setFeaturesCol("features")
      .setLabelCol("price")

    val lrModel = lr.fit(vecTrainDF) // lr.fit() returns a LinearRegressionModel (lrModel), which is a transformer.

    /**
      * Once the estimator has learned the parameters, the transformer can apply these parameters to new data points to generate predictions.
      */

    val m = lrModel.coefficients(0)
    val b = lrModel.intercept
    println(f"""The formula for the linear regression line is price = $m%1.2f*bedrooms + $b%1.2f""")

  }
}
