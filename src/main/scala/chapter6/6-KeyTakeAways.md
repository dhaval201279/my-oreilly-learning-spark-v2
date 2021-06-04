#Costs of Using Datasets
In “DataFrames Versus Datasets” in Chapter 3, we outlined some of the benefits of using Datasets—but these benefits come at a cost. As noted in the preceding section, when Datasets are passed to higher-order functions such as filter(), map(), or flatMap() that take lambdas and functional arguments, there is a cost associated with deserializing from Spark’s internal Tungsten format into the JVM object.

Compared to other serializers used before encoders were introduced in Spark, this cost is minor and tolerable. However, over larger data sets and many queries, this cost accrues and can affect performance.

##Strategies to Mitigate Costs
1. One strategy to mitigate excessive serialization and deserialization is to use DSL expressions in your queries and avoid excessive use of lambdas as anonymous functions as arguments to higher-order functions. Because lambdas are anonymous and opaque to the Catalyst optimizer until runtime, when you use them it cannot efficiently discern what you’re doing (you’re not telling Spark what to do) and thus cannot optimize your queries
2. The second strategy is to chain your queries together in such a way that serialization and deserialization is minimized. Chaining queries together is a common practice in Spark.
`personDS
 
   // Everyone above 40: lambda-1
   .filter(x => x.birthDate.split("-")(0).toInt > earliestYear)
   
   // Everyone earning more than 80K
   .filter($"salary" > 80000)
   
   // Last name starts with J: lambda-2
   .filter(x => x.lastName.startsWith("J"))
   
   // First name starts with D
   .filter($"firstName".startsWith("D"))
   .count()
`
In above code each time we move from lambda to DSL (filter($"salary" > 8000)) we incur the cost of serializing and deserializing the Person JVM object

the below query uses only DSL and no lambdas. As a result, it’s much more efficient—no serialization/deserialization is required for the entire composed and chained query
`personDS
   .filter(year($"birthDate") > earliestYear) // Everyone above 40
   .filter($"salary" > 80000) // Everyone earning more than 80K
   .filter($"lastName".startsWith("J")) // Last name starts with J
   .filter($"firstName".startsWith("D")) // First name starts with D
   .count()
`
