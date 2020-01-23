package edu.nyu.optim

import scala.math.random
import org.apache.spark.sql.SparkSession
import breeze.linalg//{DenseVector, Vector}
//import org.apache.spark.mllib.linalg.{DenseVector, Vector, Vectors}
object Jacobi2 {
 
  def appMain(spark: SparkSession,  args: Array[String]) {
     val sc = spark.sparkContext
     val N = args(0).toInt
     val max_iters = args(1).toInt 
     val parallelism = args(2).toInt 

     val tol : Double =1e-5
     val h : Double =1.0/(N+1)    
     val hsq: Double =h*h
     val hsqHalf: Double = 0.5*hsq
     val invhsq : Double = 1/hsq
     var res: Double= 1
     var k=0//iteration
     var ress= Array.fill(max_iters)(0.0)       
     val time1 = System.currentTimeMillis
     val numTasks = parallelism;
          
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
     var u_rdd =sc.parallelize( u0, parallelism)
     

      
      while (k < max_iters && (res > tol)) {    
       
//        RunJacobi.report("computing A*u_k")
          val Auk_rdd = A_rdd.join(u_rdd) // join with vector u's elements
              .flatMap{case(k, v) => v._1.map(mv => (mv._1, mv._2 * v._2))}// multiply with vector elements                    
              .reduceByKey(_ + _) // add Q values by row
           
          
//        RunJacobi.report("computing u_{k+1} = 0.5*hsq-0.5A*u_k+u_k")
          u_rdd = Auk_rdd.mapValues(hsqHalf - 0.5 * _)
                 .join(u_rdd)
                 .mapValues{ s =>s._1 + s._2}
           
//        u_rdd.collect().foreach { case(k, v) => println(k+ ", value = "+v) }
//        RunJacobi.report("computing A*u_{k+1}")
          val Aukplus1_rdd = A_rdd.join(u_rdd) // join with vector u's elements
              .flatMap{case(k, v) => v._1.map(mv => (mv._1, mv._2 * v._2))}// multiply with vector elements                      
              .reduceByKey(_ + _) // add Q values by row

          //Aukplus1_rdd.collect().foreach { case(k, v) => println("i = "+ k+ ", value = "+v) }
              
//        RunJacobi.report("computing residual")          
          res = Math.sqrt(
              Aukplus1_rdd
              .mapValues(invhsq*_ - 1)//  f is vector 1
              .map{ case(k, v) => v * v}
              .reduce(_ + _)) 
          ress(k) = res
          println("________________END OF ITERATION = "+k+"\n________________RES= "+res)
          k+=1
      }
      val time2 = System.currentTimeMillis
      val elaptime = (time2-time1)/1000.0
      /** uncomment on Dumbo
      val outputFile = "jac2_"+N+"_"+parallelism      
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
