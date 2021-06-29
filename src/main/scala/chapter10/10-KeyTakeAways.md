# Machine Learning

## Supervised Learning
In supervised machine learning, your data consists of a set of input records, each of which has associated labels, and the goal is to predict the output label(s) given a new unlabeled input. These output labels can either be discrete or continuous, which brings us to the two types of supervised machine learning: 
1. Classification 
2. Regression

## Popular classification and regression algorithm
| Algorithm                      | Typical Usage                                                                 |
|:------------------------------:|:---------------------------------------------------------|
| Linear regression              | Regression |
| Logistic regression            | Classification (we know, it has regression in the name!) |
| Decision trees                 | Both |
| Gradient boosted trees         | Both |
| Random forests                 | Both |
| Naive Bayes                    | Classification |
| Support vector machines (SVMs) | Classification |

## UnSupervised Learning
Obtaining the labeled data required by supervised machine learning can be very expensive and/or infeasible. This is where unsupervised 
machine learning comes into play. Instead of predicting a label, unsupervised ML helps you to better understand the structure of your data.

Unsupervised machine learning can be used for outlier detection or as a preprocessing step for supervised machine learning—for example, 
to reduce the dimensionality (i.e., number of dimensions per datum) of the data set, which is useful for reducing storage requirements or 
simplifying downstream tasks. Some unsupervised machine learning algorithms in MLlib include k-means, Latent Dirichlet Allocation (LDA), 
and Gaussian mixture models.

## Creating training and test data sets
many data scientists use 80/20 as a standard train/test split.

## Understanding linear regression
Linear regression models a linear relationship between your dependent variable (or label) and one or more independent variables 
(or features). In our case, we want to fit a linear regression model to predict the price of an Airbnb rental given the number of bedrooms.

### Using estimators to build models
In Spark, LinearRegression is a type of estimator—it takes in a DataFrame and returns a Model. Estimators learn parameters from your data, 
have an `estimator_name.fit()` method, and are eagerly evaluated (i.e., kick off Spark jobs), whereas transformers are lazily evaluated. 
Some other examples of estimators include `Imputer`, `DecisionTreeClassifier`, and `RandomForestRegressor`
``` java 
// In Scala
import org.apache.spark.ml.regression.LinearRegression
val lr = new LinearRegression()
  .setFeaturesCol("features")
  .setLabelCol("price")

val lrModel = lr.fit(vecTrainDF)
```

`lr.fit()` returns a `LinearRegressionModel` (lrModel), which is a transformer. In other words, the output of an estimator’s `fit()` method 
is a transformer. Once the estimator has learned the parameters, the transformer can apply these parameters to new data points to generate 
predictions. Let’s inspect the parameters it learned (Refer below code):

``` java 
// In Scala
val m = lrModel.coefficients(0)
val b = lrModel.intercept
println(f"""The formula for the linear regression line is price = $m%1.2f*bedrooms + $b%1.2f""")
```

### Creating a pipeline
Motivation for the Pipeline API: It simply allows you to specify the stages you want your data to pass through, in order, and Spark takes 
care of the processing for you. They provide the user with better code reusability and organization. In Spark, Pipelines are estimators, 
whereas PipelineModels—fitted Pipelines—are transformers.

Another advantage of using the Pipeline API is that it determines which stages are estimators/transformers for you, so you don’t have to 
worry about specifying `name.fit()` versus `name.transform()` for each of the stages.

``` java 
// In Scala
import org.apache.spark.ml.Pipeline
val pipeline = new Pipeline().setStages(Array(vecAssembler, lr))
val pipelineModel = pipeline.fit(trainDF)

val predDF = pipelineModel.transform(testDF)
predDF.select("bedrooms", "features", "price", "prediction").show(10)
```

# Hyperparameter tuning
A hyperparameter is an attribute that you define about the model prior to training, and it is not learned during the training process 
(not to be confused with parameters, which are learned in the training process). The number of trees in your random forest is an example 
of a hyperparameter.

## Tree based models

### Decision Trees
A decision tree is a series of if-then-else rules learned from your data for classification or regression tasks. Suppose we are trying to 
build a model to predict whether or not someone will accept a job offer, and the features comprise salary, commute time, free coffee, etc.

Tree-based methods can naturally handle categorical variables. In spark.ml, you just need to pass the categorical columns to the 
StringIndexer, and the decision tree can take care of the rest.