# STRUCTURED STREAMING

- DStream API was built upon Spark’s batch RDD API. Therefore, DStreams had the same functional semantics and fault-tolerance model as RDDs
- Structured Streaming will automatically convert this batch-like query to a streaming execution plan. This is called incrementalization: Structured Streaming figures out what state needs to be maintained to update the result each time a record arrives.

## Structured Streaming processing model
Structured Streaming provides three output modes :
1. _Append Mode_ - Only the new rows appended to the result table since the last trigger will be written to the external storage. This is applicable only in queries where existing rows in the result table cannot change (e.g., a map on an input stream).
2. _Updated Mode_ - Only the rows that were updated in the result table since the last trigger will be changed in the external storage. This mode works for output sinks that can be updated in place, such as a MySQL table.
3. _Complete Mode_ - The entire updated result table will be written to external storage.

## 5 Steps to define Streaming query

1. Define input source
2. Transform Data
    - *_Stateless Transformation_* : Operations like select(), filter(), map(), etc. do not require any information from previous rows to process the next row; each row can be processed by itself. The lack of previous “state” in these operations make them stateless. Stateless operations can be applied to both batch and streaming DataFrames.
    - *Statefull Transformation*  : In contrast, an aggregation operation like count() requires maintaining state to combine data across multiple rows. More specifically, any DataFrame operations involving grouping, joining, or aggregating are stateful transformations.
3. Define Output Sink
    - Uses above output modes
4. Specify processing details
    - Triggering Details : There are 4 options -
        * Default - When the trigger is not explicitly specified, then by default, the streaming query executes data in micro-batches where the next micro-batch is triggered as soon as the previous micro-batch has completed.
        * Processing time with trigger interval - You can explicitly specify the ProcessingTime trigger with an interval, and the query will trigger micro-batches at that fixed interval.
        * Once - In this mode, the streaming query will execute exactly one micro-batch—it processes all the new data available in a single batch and then stops itself. This is useful when you want to control the triggering and processing from an external scheduler that will restart the query using any custom schedule (e.g., to control cost by only executing a query once per day).
        * Continuous - Experimental feature
    - Checkpoint Location - This is a directory in any HDFS-compatible filesystem where a streaming query saves its progress information—that is, what data has been successfully processed. Upon failure, this metadata is used to restart the failed query exactly where it left off. Therefore, setting this option is necessary for failure recovery with exactly-once guarantees
5. Start the query - `start()` is a non blocking operation. If you want the main thread to block until the streaming query has terminated, you can use streamingQuery.awaitTermination(). If the query fails in the background with an error, awaitTermination() will also fail with that same exception. stop the query with `streamingQuery.stop()`

## Recovering from Failures with Exactly-Once Guarantees
Structured Streaming can ensure end-to-end exactly-once guarantees when the following conditions have been satisfied :
1. **Replayable streaming sources** - The data range of the last incomplete micro-batch can be reread from the source.
2. **Deterministic computations** - All data transformations deterministically produce the same result when given the same input data.
3. **Idempotent streaming sink** - The sink can identify reexecuted micro-batches and ignore duplicate writes that may be caused by restarts.

## Monitoring
*Get current metrics using StreamingQuery* - `lastProgress()` returns information on the last completed micro-batch

### Pushing metrics to dropwizard
Explicitly set the SparkSession configuration `spark.sql.streaming.metricsEnabled` to true before starting your query

### Publishing custom metrics
StreamingQueryListener is an event listener interface with which you can inject arbitrary logic to continuously publish metrics. This developer API is available only in Scala/Java.

## Reading from files
Key points to remember when using files:
* All the files must be of the same format and are expected to have the same schema. For example, if the format is "json", all the files must be in the JSON format with one JSON record per line. The schema of each JSON record must match the one specified with readStream(). Violation of these assumptions can lead to incorrect parsing (e.g., unexpected null values) or query failures.
* Each file must appear in the directory listing atomically—that is, the whole file must be available at once for reading, and once it is available, the file cannot be updated or modified. This is because Structured Streaming will process the file when the engine finds it (using directory listing) and internally mark it as processed. Any changes to that file will not be processed.
* When there are multiple new files to process but it can only pick some of them in the next micro-batch (e.g., because of rate limits), it will select the files with the earliest timestamps. Within the micro-batch, however, there is no predefined order of reading of the selected files; all of them will be read in parallel.

## Writing to files
Supports only append mode.

Key points to remember:
* Structured Streaming achieves end-to-end exactly-once guarantees when writing to files by maintaining a log of the data files that have been written to the directory. This log is maintained in the subdirectory _spark_metadata. Any Spark query on the directory (not its subdirectories) will automatically use the log to read the correct set of data files so that the exactly-once guarantee is maintained (i.e., no duplicate data or partial files are read). Note that other processing engines may not be aware of this log and hence may not provide the same guarantee.
* If you change the schema of the result DataFrame between restarts, then the output directory will have data in multiple schemas. These schemas have to be reconciled when querying the directory.

## Custom streaming sources and sink
Two operations that allow you to write the output of a streaming query to arbitrary storage systems: `foreachBatch()` and `foreach()`. 
They have slightly different use cases: while foreach() allows custom write logic on every row, foreachBatch() allows arbitrary operations and custom logic on the output of each micro-batch.

### Writing to multiple locations
Each attempt to write can cause the output data to be recomputed (including possible rereading of the input data). To avoid recomputations, you should cache the batchOutputDataFrame, write it to multiple locations, and then uncache it

```java
// In Scala
def writeCountsToMultipleLocations(
  updatedCountsDF: DataFrame, 
  batchId: Long) {
    updatedCountsDF.persist()
    updatedCountsDF.write.format(...).save()  // Location 1
    updatedCountsDF.write.format(...).save()  // Location 2
    updatedCountsDF.unpersist()
 }
```

## Data Transformations
Each execution is considered as a micro-batch, and the partial intermediate result that is communicated between the executions is called the streaming *state*.
DataFrame operations can be broadly classified into 
1. Stateless
2. Statefull

### Stateless Transformations
All projection operations (e.g., `select()`, `explode()`, `map()`, `flatMap()`) and selection operations (e.g., `filter()`, `where()`) process each input record individually without needing any information from previous rows. This lack of dependence on prior input data makes them stateless operations.
A streaming query having only stateless operations supports the append and update output modes, but not complete mode.

### Statefull Transformations
Simplest example of a stateful transformation is `DataFrame.groupBy().count()`
For all stateful operations, Structured Streaming ensures the correctness of the operation by automatically saving and restoring the state in a distributed manner.

Stateful transformations are further categorized into - 
- Managed Stateful Operations : automatically identify and clean up old state. The operations that fall into this category are those for:
    * Streaming aggregations
    * Stream–stream joins
    * Streaming deduplication
- Unmanaged Stateful Operations : These operations let you define your own custom state cleanup logic. The operations in this category are:
    * MapGroupsWithState
    * FlatMapGroupsWithState

#### Stateful Streaming Aggregations

#####  Aggregations not based on time
of 2 types - 
* Global Aggregation - E.g. 
``` java
// In Scala
val runningCount = sensorReadings.groupBy().count()
```
> Note - You cannot use direct aggregation operations like `DataFrame.count()` and `Dataset.reduce()` on streaming DataFrames. This is because, for static DataFrames, these operations immediately return the final computed aggregates, whereas for streaming DataFrames the aggregates have to be continuously updated. Therefore, you have to always use `DataFrame.groupBy()` or `Dataset.groupByKey()` for aggregations on streaming DataFrames.
* Grouped Aggregation - E.g.
``` java
// In Scala
val baselineValues = sensorReadings.groupBy("sensorId").mean("value")
```
built-in aggregation functions `sum()`, `mean()`, `stddev()`, `countDistinct()`, `collect_set()`, `approx_count_distinct()`, etc. can be used
You can apply multiple aggregation functions to be computed together in the following manner:
``` java
// In Scala
import org.apache.spark.sql.functions.*
val multipleAggs = sensorReadings
  .groupBy("sensorId")
  .agg(count("*"), mean("value").alias("baselineValue"),
    collect_set("errorCode").alias("allErrorCodes"))
```

#####  Aggregations with event-time windows
We can express this five-minute count as -
``` java
// In Scala
import org.apache.spark.sql.functions.*
sensorReadings
  .groupBy("sensorId", window("eventTime", "5 minute"))
  .count()
```

if you want to compute counts corresponding to 10-minute windows sliding every 5 minutes, then you can do the following:
``` java 
// In Scala
sensorReadings
  .groupBy("sensorId", window("eventTime", "10 minute", "5 minute"))
  .count()
```

##### Handling late data with watermarks
A **watermark** is defined as a moving threshold in event time that trails behind the maximum event time seen by the query in the processed data. The trailing gap, known as the watermark delay, defines how long the engine will wait for late data to arrive.

E.g. -
``` java
// In Scala
sensorReadings
  .withWatermark("eventTime", "10 minutes")
  .groupBy("sensorId", window("eventTime", "10 minutes", "5 minute"))
  .mean("value")
``` 
Explanation -
* You must call `withWatermark()` before the `groupBy()` and on the same timestamp column as that used to define windows. When this query is executed, Structured Streaming will continuously track the maximum observed value of the eventTime column and accordingly update the watermark, filter the “too late” data, and clear old state.
* Any data late by more than 10 minutes will be ignored, and all time windows that are more than 10 minutes older than the latest (by event time) input data will be cleaned up from the state

#### Streaming Joins
1. Inner equi-join
``` java
// In Scala
val matched = clicksStream.join(impressionsStatic, "adId")
```
2. Left outer join
``` java
// In Scala
val matched = clicksStream.join(impressionsStatic, Seq("adId"), "leftOuter")
```

Key points about stream-static joins -
1. Stream–static joins are stateless operations, and therefore do not require any kind of watermarking.
2. The static DataFrame is read repeatedly while joining with the streaming data of every micro-batch, so you can cache the static DataFrame to speed up the reads.
3. If the underlying data in the data source on which the static DataFrame was defined changes, whether those changes are seen by the streaming query depends on the specific behavior of the data source. For example, if the static DataFrame was defined on files, then changes to those files (e.g., appends) will not be picked up until the streaming query is restarted.

##### Stream - Stream Joins
To limit the streaming state maintained by stream–stream joins, you need to know the following information about your use case:
1. What is the maximum time range between the generation of the two events at their respective sources?
2. What is the maximum duration an event can be delayed in transit between the source and the processing engine?

Few key points to remember about inner joins:
- For inner joins, specifying watermarking and event-time constraints are both optional. In other words, at the risk of potentially unbounded state, you may choose not to specify them. Only when both are specified will you get state cleanup.
- Similar to the guarantees provided by watermarking on aggregations, a watermark delay of two hours guarantees that the engine will never drop or not match any data that is less than two hours delayed, but data delayed by more than two hours may or may not get processed.

Few additional points to note about outer joins:
- Unlike with inner joins, the watermark delay and event-time constraints are not optional for outer joins. This is because for generating the NULL results, the engine must know when an event is not going to match with anything else in the future. For correct outer join results and state cleanup, the watermarking and event-time constraints must be specified.
- Consequently, the outer NULL results will be generated with a delay as the engine has to wait for a while to ensure that there neither were nor would be any matches. This delay is the maximum buffering time (with respect to event time) calculated by the engine for each event as discussed in the previous section (i.e., four hours for impressions and two hours for clicks).

### Arbitrary Stateful Computations

#### With `mapGroupsWithState()`
define a function with the following signature (K, V, S, and U are data types, as explained shortly):
``` java
// Definition In Scala
def arbitraryStateUpdateFunction(
    key: K, 
    newDataForKey: Iterator[V], 
    previousStateForKey: GroupState[S]
): U

// Usage in Scala
// In Scala
val inputDataset: Dataset[V] =  // input streaming Dataset

inputDataset
  .groupByKey(keyFunction)   // keyFunction() generates key from input
  .mapGroupsWithState(arbitraryStateUpdateFunction)
```

Few notable points to remember:
1. When the function is called, there is no well-defined order for the input records in the new data iterator (e.g., newActions). If you need to update the state with the input records in a specific order (e.g., in the order the actions were performed), then you have to explicitly reorder them (e.g., based on the event timestamp or some other ordering ID). In fact, if there is a possibility that actions may be read out of order from the source, then you have to consider the possibility that a future micro-batch may receive data that should be processed before the data in the current batch. In that case, you have to buffer the records as part of the state.
2. In a micro-batch, the function is called on a key once only if the micro-batch has data for that key. For example, if a user becomes inactive and provides no new actions for a long time, then by default, the function will not be called for a long time. If you want to update or remove state based on a user’s inactivity over an extended period you have to use timeouts, which we will discuss in the next section.
3. The output of `mapGroupsWithState()` is assumed by the incremental processing engine to be continuously updated key/value records, similar to the output of aggregations. This limits what operations are supported in the query after `mapGroupsWithState()`, and what sinks are supported. For example, appending the output into files is not supported. If you want to apply arbitrary stateful operations with greater flexibility, then you have to use `flatMapGroupsWithState()`. We will discuss that after timeouts.

## Performance Tuning
Few considerations to keep in mind:
1. Cluster resource provisioning -  allocation should be done based on the nature of the streaming queries: stateless queries usually need more cores, and stateful queries usually need more memory
2. Number of partitions for shuffles - 
For Structured Streaming queries, the number of shuffle partitions usually needs to be set much lower than for most batch queries—dividing the computation too much increases overheads and reduces throughput.
for streaming queries with stateful operations and trigger intervals of a few seconds to minutes, it is recommended to tune the number of shuffle partitions from the default value of 200 to at most two to three times the number of allocated cores.
3. Setting source rate limits for stability - Setting limits in supported sources (e.g., Kafka and files) prevents a query from consuming too much data in a single micro-batch. The surge data will stay buffered in the source, and the query will eventually catch up.
4. Multiple streaming queries in the same Spark application - 
Running multiple streaming queries in the same SparkContext or SparkSession can lead to fine-grained resource sharing.
> You can ensure fairer resource allocation between queries in the same context by setting them to run in separate scheduler pools. Set the SparkContext’s thread-local property spark.scheduler.pool to a different string value for each stream

