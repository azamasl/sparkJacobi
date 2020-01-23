package edu.nyu.optim

import org.apache.spark.sql.SparkSession
import org.apache.spark.rdd.RDD.rddToPairRDDFunctions

//import org.apache.spark.mllib.linalg.{DenseVector, Vector, Vectors}
object Jacobi3 {
 
  def appMain(spark: SparkSession,  args: Array[String]) {
     val sc = spark.sparkContext
     val N = args(0).toInt
     val max_iters = args(1).toInt 
     val parallelism = args(2).toInt 
     val numTasks = parallelism;
     val tol : Double =1e-5
     val h : Double =1.0/(N+1)    
     val hsq: Double =h*h
     val hsqHalf: Double = 0.5*hsq
     val invhsq : Double = 1/hsq
     var res: Double= 1
     var k=0//iteration
     var ress= Array.fill(max_iters)(0.0)       
     val time1 = System.currentTimeMillis
         
     var A0 : List[(Long,(Long,Double))] = List()
     A0=A0:+(0L, (0L, 2.0)):+(0L, (1L, -1.0)) //First column
     for(col <- 1 until N-1){
       A0=A0 :+ (col.toLong, ((col-1).toLong, -1.0)) :+ (col.toLong, (col.toLong, 2.0)):+ (col.toLong, ((col+1).toLong, -1.0))  
     }      
     val col = N-1//Last column
     A0=A0 :+ (col.toLong, ((col-1).toLong, -1.0)) :+ (col.toLong, (col.toLong, 2.0))                 
     val A_rdd =sc.parallelize( A0, parallelism).groupByKey().cache()
    
     var u0 : List[(Long,Double)] = List()
     for(col <- 0 until N){ u0=u0:+(col.toLong, 0.0)}    
     var u_rdd =sc.parallelize(u0)

     var u_recie = sc.broadcast(u_rdd.collect().toMap).value       
//   Computing A*u_0
     var Auk_rdd = A_rdd.mapPartitions({ iter =>
     for {
        (ak, av) <- iter
      } yield (ak, (av, u_recie.get(ak).get))
     }, preservesPartitioning = true)
          .flatMap{case(k, v) => v._1.map(Av => (Av._1, Av._2 * v._2))}                   
          .reduceByKey(_ + _,numTasks).cache()//reduce on rows
     
     while (k < max_iters && (res > tol)) {         
//        Computing u_{k+1} = 0.5*hsq-0.5A*u_k+u_k
           u_rdd = Auk_rdd.mapValues(hsqHalf - 0.5 * _)
                 .mapPartitions({ iter =>
                   for {(ak, av) <- iter
                     } yield (ak, (av, u_recie.get(ak).get))
                   },preservesPartitioning = true)
                 .mapValues{ s =>s._1 + s._2}.cache()                                      
           u_recie = sc.broadcast(u_rdd.collect().toMap).value                       
//         Computing A*u_{k+1}
           Auk_rdd = A_rdd
                  .mapPartitions({ iter =>
                    for {(ak, av) <- iter
                      } yield (ak, (av, u_recie.get(ak).get))
                    },preservesPartitioning = true)
                  .flatMap{case(k, v) => v._1.map(Av => (Av._1, Av._2 * v._2))}                  
                  .reduceByKey(_ + _,numTasks).cache()//reduce on rows
              
//         Computing residual
           val accum = sc.doubleAccumulator("Accumulating residual")
           Auk_rdd.mapValues(invhsq*_ - 1)             
                  .map{ case(k, v) => v * v}
                  .foreach(x => accum.add(x))    
          ress(k) = Math.sqrt(accum.value)
          k+=1
      }
      val time2 = System.currentTimeMillis
      val elaptime = (time2-time1)/1000.0
      /** uncomment on Dumbo
      val outputFile = "jac3_"+N+"_"+parallelism      
      val outputData = ("\n Max_iter = " + max_iters + "\n N = "+ N +" Time_elapsed(s) = "+ elaptime + " \n " + ress.mkString("\n ") )      
      val hadoopConf = new org.apache.hadoop.conf.Configuration()
      val hdfs = org.apache.hadoop.fs.FileSystem.get(new java.net.URI("hdfs://babar.es.its.nyu.edu:8020/user/aa2821/"), hadoopConf)
      try { hdfs.delete(new org.apache.hadoop.fs.Path(outputFile), true) } catch { case _ : Throwable => { } }
      sc.parallelize(List((outputData)),1).saveAsTextFile(outputFile)
 */

      println("\n Number of iterations	 = "+ k)  
      println(ress.mkString("\n")) 
      println("\n Time elapsed is "+ elaptime +" seconds.") 
        
  }
}


     //val un = Array.fill[Double](N)(0.0)
     //println(un.mkString("\n")) 
     
    // val broadcastU = sc.broadcast(u_rdd.cache())
     //val Urecived = broadcastU.value
     //println(Urecived.mkString("\n")) 