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