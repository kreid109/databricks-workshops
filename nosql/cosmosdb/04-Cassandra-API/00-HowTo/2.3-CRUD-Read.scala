// Databricks notebook source
// MAGIC %md
// MAGIC # What's in this exercise
// MAGIC Basics of how to work with CosmosDB from Databricks <B>in batch</B>.<BR>
// MAGIC Section 04: Read operation (cRud)<BR>
// MAGIC   Aggregation operations covered separately<br>
// MAGIC 
// MAGIC **Reference:**<br> 
// MAGIC **TODO**
// MAGIC   
// MAGIC   implicit val readConf = ReadConf.fromSparkConf(sc.getConf).copy(
// MAGIC     consistencyLevel = ConsistencyLevel.ALL)

// COMMAND ----------

// MAGIC %md
// MAGIC ## 4.0. Read operation (C*R*UD)

// COMMAND ----------

// MAGIC %run ../00-HowTo/2-CassandraUtils

// COMMAND ----------

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

import spark.implicits._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.Column
import org.apache.spark.sql.types.{StructType, StructField, StringType, IntegerType,LongType,FloatType,DoubleType, TimestampType}
import org.apache.spark.sql.cassandra._

//datastax Spark connector
import com.datastax.spark.connector._
import com.datastax.spark.connector.cql.CassandraConnector

//CosmosDB library for multiple retry
import com.microsoft.azure.cosmosdb.cassandra

// Specify connection factory for Cassandra
spark.conf.set("spark.cassandra.connection.factory", "com.microsoft.azure.cosmosdb.cassandra.CosmosDbConnectionFactory")

// Parallelism and throughput configs
spark.conf.set("spark.cassandra.output.batch.size.rows", "1")
spark.conf.set("spark.cassandra.connection.connections_per_executor_max", "10")
spark.conf.set("spark.cassandra.output.concurrent.writes", "100")
spark.conf.set("spark.cassandra.concurrent.reads", "512")
spark.conf.set("spark.cassandra.output.batch.grouping.buffer.size", "1000")
spark.conf.set("spark.cassandra.connection.keep_alive_ms", "60000000") //Increase this number as needed
spark.conf.set("spark.cassandra.output.consistency.level","ALL")//Write consistency = Strong
spark.conf.set("spark.cassandra.input.consistency.level","ALL")//Read consistency = Strong

// COMMAND ----------

// MAGIC %md
// MAGIC #### 4.0.1. Dataframe API

// COMMAND ----------

// MAGIC %md
// MAGIC ##### 4.0.1.1. Read using session.read.format("org.apache.spark.sql.cassandra")

// COMMAND ----------

//spark.read with format("org.apache.spark.sql.cassandra")
val readBooksDF = sqlContext
  .read
  .format("org.apache.spark.sql.cassandra")
  .options(Map( "table" -> "books", "keyspace" -> "books_ks"))
  .load

readBooksDF.explain
readBooksDF.show

// COMMAND ----------

// MAGIC %md
// MAGIC ##### 4.0.1.2. Read using spark.read with read.cassandraFormat(...)

// COMMAND ----------

//spark.read with read.cassandraFormat(...)
val readBooksDF = spark
  .read
  .cassandraFormat("books", "books_ks", "")
  .load()

// COMMAND ----------

// MAGIC %md
// MAGIC ##### 4.0.1.3. Projection and predicate pushdown

// COMMAND ----------


//Projection and predicate pushdown
val readBooksDF = spark
  .read
  .format("org.apache.spark.sql.cassandra")
  .options(Map( "table" -> "books", "keyspace" -> "books_ks"))
  .load
  .select("book_name","book_author", "book_pub_year")
  .filter("book_pub_year > 1891")
//.filter("book_name IN ('A sign of four','A study in scarlet')")
//.filter("book_name='A sign of four' OR book_name='A study in scarlet'")
//.filter("book_author='Arthur Conan Doyle' AND book_pub_year=1890")
//.filter("book_pub_year=1903")  


readBooksDF.explain
readBooksDF.show

// COMMAND ----------

// MAGIC %md
// MAGIC #### 4.0.2. RDD API

// COMMAND ----------

// MAGIC %md
// MAGIC ##### 4.0.2.1. Read 

// COMMAND ----------

//Just an example - use collect wisely or not use at all
val bookRDD = sc.cassandraTable("books_ks", "books").collect.foreach(println)

// COMMAND ----------

// MAGIC %md
// MAGIC ##### 4.0.2.2. Projection and predicate pushdown

// COMMAND ----------

//sc.cassandraTable("books_ks", "books").select("book_name","book_author","book_pub_year").where("book_pub_year > ?", 1891).collect.foreach(println)
sc.cassandraTable("books_ks", "books").select("book_id","book_author").where("book_name = ?", "A sign of four").collect.foreach(println)


// COMMAND ----------

// MAGIC %md
// MAGIC ####4.0.3 Create view and read off of the same

// COMMAND ----------

spark
  .read
  .format("org.apache.spark.sql.cassandra")
  .options(Map( "table" -> "books", "keyspace" -> "books_ks"))
  .load.createOrReplaceTempView("books_vw")

// COMMAND ----------

// MAGIC %md
// MAGIC **Explore the data running queries below:**

// COMMAND ----------

// MAGIC %sql
// MAGIC select * from books_vw;
// MAGIC --select * from books_vw where book_pub_year > 1891