package edu.nyu.optim

import java.util.Calendar
import java.text.SimpleDateFormat

import scala.math.random
import org.apache.spark.sql.SparkSession
import breeze.linalg//{DenseVector, Vector}


/** Computes an approximation to pi by monte carlo method*/
object RunJacobi2 {
  
    def report(message: String, verbose: Boolean = true) {
        val now = Calendar.getInstance().getTime()
        val formatter = new SimpleDateFormat("H:m:s")
        if (verbose) {
            println("\n STATUS REPORT (" + formatter.format(now) + "): " + message)
        }
    }
    def main(args: Array[String]) {
    
     val spark = SparkSession
      .builder()
      .appName("Jacobi2")
      .master("local[*]")
      .getOrCreate()  
       
       //sys.addShutdownHook( { spark.stop() } )
       Jacobi2.appMain(spark, args)
    }
}
 

