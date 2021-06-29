# Dynamic Partition Pruning
The idea behind dynamic partition pruning (DPP) is to skip over the data you don’t need in a query’s results. The typical scenario where 
DPP is optimal is when you are joining two tables: a fact table (partitioned over multiple columns) and a dimension table (nonpartitioned)

The key optimization technique in DPP is to take the result of the filter from the dimension table and inject it into the fact table as 
part of the scan operation to limit the data read

# Adaptive Query Execution
It attempts to to do the following at runtime:
- Reduce the number of reducers in the shuffle stage by decreasing the number of shuffle partitions.
- Optimize the physical execution plan of the query, for example by converting a SortMergeJoin into a BroadcastHashJoin where appropriate.
- Handle data skew during a join.

Two Spark SQL configurations dictate how AQE will reduce the number of reducers:
- `spark.sql.adaptive.coalescePartitions.enabled` (set to true)
- `spark.sql.adaptive.skewJoin.enabled` (set to true)

# SQL Join Hints
## SHUFFLE SORT MERGE JOIN (SMJ)
perform a SortMergeJoin when joining tables a and b or customers and orders, as shown in the following examples
``` java 
SELECT /*+ MERGE(a, b) */ id FROM a JOIN b ON a.key = b.key
SELECT /*+ MERGE(customers, orders) */ * FROM customers, orders WHERE 
    orders.custId = customers.custId
```

## BROADCAST HASH JOIN (BHJ)
here we broadcast table a to join with table b and table customers to join with table orders:
``` java 
SELECT /*+ BROADCAST(a) */ id FROM a JOIN b ON a.key = b.key
SELECT /*+ BROADCAST(customers) */ * FROM customers, orders WHERE 
    orders.custId = customers.custId
```

## SHUFFLE HASH JOIN (SHJ)
less commonly used as compared to above two
``` java 
SELECT /*+ SHUFFLE_HASH(a, b) */ id FROM a JOIN b ON a.key = b.key
SELECT /*+ SHUFFLE_HASH(customers, orders) */ * FROM customers, orders WHERE 
    orders.custId = customers.custId
```

## SHUFFLE-AND-REPLICATE NESTED LOOP JOIN (SNLJ)
``` java 
SELECT /*+ SHUFFLE_REPLICATE_NL(a, b) */ id FROM a JOIN b
```

# Structured Streaming
all configurations works straight out of the Spark 3.0 installation, with the following defaults:
- `spark.sql.streaming.ui.enabled=true`
- `spark.sql.streaming.ui.retainedProgressUpdates=100`
- `spark.sql.streaming.ui.retainedQueries=100`