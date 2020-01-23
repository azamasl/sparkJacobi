 package edu.nyu.optim

import org.apache.spark.sql.SparkSession
import org.apache.spark.mllib.linalg.{Matrix, Vectors, Matrices}
import org.apache.spark.mllib.linalg.distributed.{BlockMatrix, MatrixEntry, CoordinateMatrix}
import breeze.linalg.DenseVector
import breeze.numerics._

object Jacobi1 {

   def appMain(spark: SparkSession,  args: Array[String]) {

     val sc = spark.sparkContext
     val N = args(0).toInt
     val max_iters = args(1).toInt 
     val parallelism = args(2).toInt 
     
     val blockSize = N

     val tol : Double =1e-5
     val h : Double =1.0/(N+1)
     val hsq: Double =h*h
     val hsqHalf: Double = 0.5*hsq
     val invhsq : Double = 1/hsq
     var res: Double= 1
     var k=0//iteration
     var ress= Array.fill(max_iters)(0.0)
     // A is square matrix
     val A_rowsPerBlock = blockSize
     val A_colsPerBlock = 1
     // u is a column vector
     val u_rowsPerBlock = 1
     val u_colsPerBlock = 1
     val time1 = System.currentTimeMillis
     
     //Matrices.sparse( A_rowsPerBlock, A_colsPerBlock,Array(0,2),Array(blockSize-2,blockSize-1),  Array(-1.0, 2.0))
     val A_rdd= sc.parallelize( { for(i <- 0 until blockSize) yield (0,i)}, parallelism)
                      .map{ coord => 
                        val i = coord._2//row number
                        if(i==0){//Matrices.sparse( A_rowsPerBlock,A_colsPerBlock,Array(0,2), Array(0,1) , Array(2.0,-1.0))) 
                            var Vd = DenseVector.fill(A_rowsPerBlock){ 0.000000000001}.toArray
                            Vd(i) = 2.0    
                            Vd(i+1)  = -1.0
                            (coord, Matrices.dense(A_rowsPerBlock, A_colsPerBlock,Vd))
                              
                        }
                        else{
                           if(i==blockSize-1){ 
                               var Vd = DenseVector.fill(A_rowsPerBlock){ 0.000000000001}.toArray
                               Vd(i-1)  = -1.0
                               Vd(i) = 2.0
                               (coord, Matrices.dense(A_rowsPerBlock, A_colsPerBlock,Vd))
                           }
                           else {//Matrices.sparse( A_rowsPerBlock, A_colsPerBlock, Array(0,3), Array(i-1,i,i+1), Array(-1.0,2.0,-1.0)))
                               var Vd = DenseVector.fill(A_rowsPerBlock){ 0.000000000001}.toArray
                               Vd(i-1)  = -1.0
                               Vd(i) = 2.0    
                               Vd(i+1)  = -1.0
                             (coord, Matrices.dense(A_rowsPerBlock, A_colsPerBlock,Vd))
                                 
                           }
                        }
                      }
      Matrices.zeros(blockSize,1)
      val A = new BlockMatrix(A_rdd, A_rowsPerBlock, A_colsPerBlock,blockSize,blockSize ).cache()
      A.validate() 
      
      val u_rdd = sc.parallelize({ for(i <- 0 until blockSize) yield (i,0)})
      .map{coord => 
          val i = coord._1//row number
          //Ridiculous!!! It returns an empty collection if I fill matrix of all zeros so I set to 10E-12: 0.000000001
         (coord, Matrices.dense(1,1,DenseVector.fill(1){ 0.000000000001}.toArray))//Matrices.zeros(u_rowsPerBlock,u_colsPerBlock)            
      }.cache()//column vector   
      
      var u = new BlockMatrix(u_rdd,u_rowsPerBlock,u_colsPerBlock, blockSize,1)
      u.validate()  
  
      while (k < max_iters && (res > tol)){

//          RunJacobiDist.report("computing A*u_k")
          var Auk = A.multiply(u)//Auk should be column vector.         
          
//          RunJacobiDist.report("computing 0.5*hsq-0.5A_i*u_k")
          val Aukcoor = new CoordinateMatrix(
            Auk.toCoordinateMatrix()
            .entries
            .map (entry => MatrixEntry(entry.i, entry.j, hsqHalf - 0.5 * entry.value)))
         
//          RunJacobiDist.report("computing u_{k+1}")
          u = Aukcoor.toBlockMatrix(u_rowsPerBlock, u_colsPerBlock).add(u)

          
//          RunJacobiDist.report("computing A*u_{k+1}")
          val Aukplus1 = A.multiply(u)  
         
          
//          RunJacobiDist.report("computing residual")
          res= math.sqrt(
               Aukplus1
              .toCoordinateMatrix()
              .entries
              .map(entry => math.pow(invhsq* entry.value - 1, 2))
              .reduce(_ + _))
          ress(k) = res
          println("________________END OF ITERATION = "+k+"\n________________RES= "+res)
          k+=1
      }
      val time2 = System.currentTimeMillis
      val elaptime = (time2-time1)/1000.0
      /**un-comment on Dumbo
      val outputFile = "jac1_"+N+"_"+parallelism      
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
