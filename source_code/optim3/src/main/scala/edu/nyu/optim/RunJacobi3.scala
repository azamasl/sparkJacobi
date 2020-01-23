package edu.nyu.optim

import java.util.Calendar
import java.text.SimpleDateFormat
import org.apache.spark.sql.SparkSession
object RunJacobi3 {
  
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
      .appName("Jacobi3")
      .master("local[*]")
      .getOrCreate()  
       
       //sys.addShutdownHook( { spark.stop() } )
       Jacobi3.appMain(spark, args)
    }

}
