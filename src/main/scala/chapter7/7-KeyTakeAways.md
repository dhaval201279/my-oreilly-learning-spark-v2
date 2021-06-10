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

## Dataframe.cache
cache() will store as many of the partitions read in memory across Spark executors as memory allows (see Figure 7-2). While a DataFrame may be fractionally cached, partitions cannot be fractionally cached (e.g., if you have 8 partitions but only 4.5 partitions can fit in memory, only 4 will be cached). However, if not all your partitions are cached, when you want to access the data again, the partitions that are not cached will have to be recomputed, slowing down your Spark job.

Look at the code on WorkingWithDataSets

>When you use cache() or persist(), the DataFrame is not fully cached until you invoke an action that goes through every record (e.g., count()). If you use an action like take(1), only one partition will be cached because Catalyst realizes that you do not need to compute all the partitions just to retrieve one record.
  
   
