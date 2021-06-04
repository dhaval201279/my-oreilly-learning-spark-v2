#EVALUATION ORDER AND NULL CHECKING IN SPARK SQL
Spark SQL (this includes SQL, the DataFrame API, and the Dataset API) does not guarantee the order of evaluation of subexpressions. For example, the following query does not guarantee that the s is NOT NULL clause is executed prior to the strlen(s) > 1 clause:
`spark.sql("SELECT s FROM test1 WHERE s IS NOT NULL AND strlen(s) > 1")`
So to perform proper null checking, it is recommended that you do the following:
1. Make the UDF itself null-aware and do null checking inside the UDF.
2. Use IF or CASE WHEN expressions to do the null check and invoke the UDF in a conditional branch.

#THE IMPORTANCE OF PARTITIONING
When transferring large amounts of data between Spark SQL and a JDBC external source, it is important to partition your data source. All of your data is going through one driver connection, which can saturate and significantly slow down the performance of your extraction, as well as potentially saturate the resources of your source system. While these JDBC properties are optional, for any large-scale operations it is highly recommended to use the properties shown in belo table

| Tables          | Are                                                                 |
|:---------------:|:------------------------------------------------------------------- |
| numPartitions   | The maximum number of partitions that can be used for parallelism in table reading and writing. This also determines the maximum number of concurrent JDBC connections. |
| partitionColumn | When reading an external source, partitionColumn is the column that is used to determine the partitions; note, partitionColumn must be a numeric, date, or timestamp column. |
| lowerBound      | Sets the minimum value of partitionColumn for the partition stride. |
| upperBound      | Sets the maximum value of partitionColumn for the partition stride. |

## Best Practices
+ A good starting point for numPartitions is to use a multiple of the number of Spark workers. For example, if you have four Spark worker nodes, then perhaps start with 4 or 8 partitions.
+ calculate the lowerBound and upperBound based on the minimum and maximum partitionColumn actual values. For example, if you choose {numPartitions:10, lowerBound: 1000, upperBound: 10000}, but all of the values are between 2000 and 4000, then only 2 of the 10 queries (one for each partition) will be doing all of the work. In this scenario, a better configuration would be {numPartitions:10, lowerBound: 2000, upperBound: 4000}.
+ Choose a partitionColumn that can be uniformly distributed to avoid data skew. For example, if the majority of your partitionColumn has the value 2500, with {numPartitions:10, lowerBound: 1000, upperBound: 10000} most of the work will be performed by the task requesting the values between 2000 and 3000. Instead, choose a different partitionColumn, or if possible generate a new one (perhaps a hash of multiple columns) to more evenly distribute your partitions.

#Higher Order Functions
## Transform
`transform(array<T>, function<T, U>): array<U>`

` // Calculate Fahrenheit from Celsius for an array of temperatures
spark.sql("""
    SELECT celsius, 
 transform(celsius, t -> ((t * 9) div 5) + 32) as fahrenheit 
  FROM tC
""").show()`

## FILTER()
`filter(array<T>, function<T, Boolean>): array<T>`

`// Filter temperatures > 38C for array of temperatures
 spark.sql("""
 SELECT celsius, 
  filter(celsius, t -> t > 38) as high 
   FROM tC
 """).show()`
 
## EXISTS()
 `exists(array<T>, function<T, V, Boolean>): Boolean`
 
 `// Is there a temperature of 38C in the array of temperatures
  spark.sql("""
  SELECT celsius, 
         exists(celsius, t -> t = 38) as threshold
    FROM tC
  """).show()`
  
## REDUCE()
  `reduce(array<T>, B, function<B, T, B>, function<B, R>)`
  
  `// Calculate average temperature and convert to F
   spark.sql("""
   SELECT celsius, 
          reduce(
             celsius, 
             0, 
             (t, acc) -> t + acc, 
             acc -> (acc div size(celsius) * 9 div 5) + 32
           ) as avgFahrenheit 
     FROM tC
   """).show()`
   
#Windowing
A window function uses values from the rows in a window (a range of input rows) to return a set of values, typically in the form of another row. With window functions, it is possible to operate on a group of rows while still returning a single value for every input row.



