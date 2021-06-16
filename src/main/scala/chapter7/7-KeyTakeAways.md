# Optimizing and Tuning Spark for Efficiency

There are 3 ways - 
1. Config files: conf/spark-defaults.conf.template, conf/log4j.properties.template, and conf/spark-env.sh.template
2. Specify Spark configurations directly in your Spark application or on the command line when submitting the application with spark-submit, using the --conf flag
   E.g. - *spark-submit --conf spark.sql.shuffle.partitions=5 --conf
          "spark.executor.memory=2g" --class main.scala.chapter7.SparkConfig_7_1 jars/main-
          scala-chapter7_2.12-1.0.jar*
3. Within Spark application itself

These configurations affect three Spark components: the Spark driver, the executor, and the shuffle service running on the executor.

## Dynamic Resource Allocation
To enable and configure dynamic allocation, you can use settings like the following.
*`spark.dynamicAllocation.enabled true
 spark.dynamicAllocation.minExecutors 2
 spark.dynamicAllocation.schedulerBacklogTimeout 1m
 spark.dynamicAllocation.maxExecutors 20
 spark.dynamicAllocation.executorIdleTimeout 2min`*
 
 Amount of memory available to each executor is controlled by *`spark.executor.memory`*
 This is divided into three sections, as depicted in png file within same folder
 
 - The default division is 60% for execution memory and 40% for storage, after allowing for 300 MB for reserved memory, to safeguard against OOM errors
   - **Execution memory** - used for Spark shuffles, joins, sorts, and aggregations. Since different queries may require different amounts of memory, the fraction (spark.memory.fraction is 0.6 by default) of the available memory to dedicate to this can be tricky to tune but it’s easy to adjust.
   - **Storage memory** - used for caching user data structures and partitions derived from DataFrames
   
 ***During map and shuffle operations, Spark writes to and reads from the local disk’s shuffle files, so there is heavy I/O activity. This can result in a bottleneck, because the default configurations are suboptimal for large-scale Spark jobs***
 
 ## Spark configurations to tweak for I/O during map and shuffle operations
 | Configuration                           | Default value, recommendation, and description                                                                 |
 |:---------------------------------------:|:------------------------------------------------------------------- |
 | spark.driver.memory                     | Default is 1g (1 GB). This is the amount of memory allocated to the Spark driver to receive data from executors. This is often changed during spark-submit with --driver-memory. Only change this if you expect the driver to receive large amounts of data back from operations like collect(), or if you run out of driver memory. |
 | spark.shuffle.file.buffer               | Default is 32 KB. Recommended is 1 MB. This allows Spark to do more buffering before writing final map results to disk. |
 | spark.file.transferTo                   | Default is true. Setting it to false will force Spark to use the file buffer to transfer files before finally writing to disk; this will decrease the I/O activity. |
 | spark.shuffle.unsafe.file.output.buffer | Default is 32 KB. This controls the amount of buffering possible when merging files during shuffle operations. In general, large values (e.g., 1 MB) are more appropriate for larger workloads, whereas the default can work for smaller workloads. |
 | spark.io.compression.lz4.blockSize      | Default is 32 KB. Increase to 512 KB. You can decrease the size of the shuffle file by increasing the compressed size of the block. |
 | spark.shuffle.service.​index.cache.size | Default is 100m. Cache entries are limited to the specified memory footprint in byte. |
 | spark.shuffle.registration.​timeout     | Default is 5000 ms. Increase to 120000 ms. |
 | spark.shuffle.registration.maxAttempts  | Default is 3. Increase to 5 if needed. |  
 
 ## Maximizing Spark Parallelism
 In data management parlance, a partition is a way to arrange data into a subset of configurable and readable chunks or blocks of contiguous data on disk. These subsets of data can be read or processed independently and in parallel, if necessary, by more than a single thread in a process. This independence matters because it allows for massive parallelism of data processing.
 
 Spark job will have many stages, and within each stage there will be many tasks. Spark will at best schedule a thread per task per core, and each task will process a distinct partition. To optimize resource utilization and maximize parallelism, the ideal is at least as many partitions as there are cores on the executor.
 
 Please refer image <Relationship of spark task  . . . > - If there are more partitions than there are cores on each executor, all the cores are kept busy. You can think of partitions as atomic units of parallelism: a single thread running on a single core can work on a single partition.
 
 ### Size of partitions
 Size of a partition in Spark is dictated by spark.sql.files.maxPartitionBytes. The default is 128 MB.
 Decreasing the size may result in what’s known as the “small file problem”—many small partition files, introducing an inordinate amount of disk I/O and performance degradation
 
 ### Shuffle partitions
 shuffle partitions are created during the shuffle stage. By default, the number of shuffle partitions is set to 200 in spark.sql.shuffle.partitions. You can adjust this number depending on the size of the data set you have, to reduce the amount of small partitions being sent across the network to executors’ tasks.
 Created during operations like groupBy() or join(), also known as wide transformations, shuffle partitions consume both network and disk I/O resources
 
 >The default value for spark.sql.shuffle.partitions is too high for smaller or streaming workloads; you may want to reduce it to a lower value such as the number of cores on the executors or less.

# Caching and persistence
In spark *`cache()`* and *`persist()`* are synonymous. The latter provides more control over how and where your data is stored—in memory and on disk, serialized and unserialized

## Dataframe.cache()
*`cache()`* will store as many of the partitions read in memory across Spark executors as memory allows (see Figure 7-2). While a DataFrame may be fractionally cached, partitions cannot be fractionally cached (e.g., if you have 8 partitions but only 4.5 partitions can fit in memory, only 4 will be cached). However, if not all your partitions are cached, when you want to access the data again, the partitions that are not cached will have to be recomputed, slowing down your Spark job.

Look at the code on WorkingWithDataSets

>When you use cache() or persist(), the DataFrame is not fully cached until you invoke an action that goes through every record (e.g., count()). If you use an action like take(1), only one partition will be cached because Catalyst realizes that you do not need to compute all the partitions just to retrieve one record.
  
## Dataframe.persist()
*`persist(StorageLevel.LEVEL)`* is nuanced, providing control over how your data is cached via StorageLevel

### Storage Levels
 | Storage Level       | Description                                                         |
 |:-------------------:|:-------------------------------------------------------------------|
 | MEMORY_ONLY         | Data is stored directly as objects and stored only in memory. |
 | MEMORY_ONLY_SER     | Data is serialized as compact byte array representation and stored only in memory. To use it, it has to be deserialized at a cost. |
 | MEMORY_AND_DISK     | Data is stored directly as objects in memory, but if there’s insufficient memory the rest is serialized and stored on disk. |
 | DISK_ONLY           | Data is serialized and stored on disk. |
 | OFF_HEAP            | Data is stored off-heap. Off-heap memory is used in Spark for storage and query execution; see “Configuring Spark executors’ memory and the shuffle service”. |
 | MEMORY_AND_DISK_SER | Like MEMORY_AND_DISK, but data is serialized when stored in memory. (Data is always serialized when stored on disk.) |  

> Note - Each StorageLevel (except OFF_HEAP) has an equivalent LEVEL_NAME_2, which means replicate twice on two different Spark executors: MEMORY_ONLY_2, MEMORY_AND_DISK_SER_2, etc. While this option is expensive, it allows data locality in two places, providing fault tolerance and giving Spark the option to schedule a task local to a copy of the data.

### Unpersist
To unpersist your cached data, just call *`DataFrame.unpersist()`*.

## Cache tables and views derived from DataFrame
you can also cache the tables or views derived from DataFrames. This gives them more readable names in the Spark UI

*`df.createOrReplaceTempView("dfTable")
  spark.sql("CACHE TABLE dfTable")
  spark.sql("SELECT count(*) FROM dfTable").show()`*
  
## When not to cache / persist
Some scenarios that may not warrant caching your DataFrames include:
- DataFrames that are too big to fit in memory
- An inexpensive transformation on a DataFrame not requiring frequent use, regardless of size

As a general rule you should use memory caching judiciously, as it can incur resource costs in serializing and deserializing, depending on the StorageLevel used.

# Spark joins
Spark has five distinct join strategies by which it exchanges, moves, sorts, groups, and merges data across executors: 
- Broadcast hash join (BHJ)
- Shuffle hash join (SHJ)
- Shuffle sort merge join (SMJ)
- Broadcast nested loop join (BNLJ)
- Shuffle-and-replicated nested loop join (a.k.a. Cartesian product join)

## Broadcast hash join
Employed when two data sets, one small (fitting in the driver’s and executor’s memory) and another large enough to ideally be spared from movement, need to be joined over certain conditions or columns.

By default Spark will use a broadcast join if the smaller data set is less than 10 MB. This configuration is set in *`spark.sql.autoBroadcastJoinThreshold`*;
Specifying a value of -1 in *`spark.sql.autoBroadcastJoinThreshold`* will cause Spark to always resort to a shuffle sort merge join

`
// Programmatically forcing broadcast ..
import org.apache.spark.sql.functions.broadcast
val joinedDF = playersDF.join(broadcast(clubsDF), "key1 === key2")
`

BHJ is the easiest and fastest join Spark offers, since it does not involve any shuffle of the data set; all the data is available locally to the executor after a broadcast. You just have to be sure that you have enough memory both on the Spark driver’s and the executors’ side to hold the smaller data set in memory.

### When to use BHJ
Use this type of join under the following conditions for maximum benefit:
- When each key within the smaller and larger data sets is hashed to the same partition by Spark
- When one data set is much smaller than the other (and within the default config of 10 MB, or more if you have sufficient memory)
- When you only want to perform an equi-join, to combine two data sets based on matching unsorted keys
- When you are not worried by excessive network bandwidth usage or OOM errors, because the smaller data set will be broadcast to all Spark executors

## Shuffle Sort Merge Joins (SSMJ)
An efficient way to merge two large data sets over a common key that is sortable, unique, and can be assigned to or stored in the same partition—that is, two data sets with a common hashable key that end up being on the same partition. From Spark’s perspective, this means that all rows within each data set with the same key are hashed on the same partition on the same executor. Obviously, this means data has to be colocated or exchanged between executors.

### When to use SSMJ
Use this type of join under the following conditions for maximum benefit:
- When each key within two large data sets can be sorted and hashed to the same partition by Spark
- When you want to perform only equi-joins to combine two data sets based on matching sorted keys
- When you want to prevent Exchange and Sort operations to save large shuffles across the network

# Inspecting Spark UI
## Jobs and Stages
The Duration column indicates the time it took for each job (identified by the Job Id in the first column) to finish. If this time is high, it’s a good indication that you might want to investigate the stages in that job to see what tasks might be causing delays.

