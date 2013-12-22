package pl.project13.scala.oculus

import pl.project13.scala.oculus.job._
import org.apache.hadoop.util._
import com.twitter.scalding
import org.apache.hadoop.conf.Configuration
import collection.JavaConversions._
import com.typesafe.config.{ConfigFactory, Config}
import org.apache.hadoop.fs.{Path, FileSystem, FileUtil}
import java.io.File
import com.google.common.base.Stopwatch
import com.twitter.scalding._
import org.apache.hadoop
import com.twitter.scalding.Hdfs
import pl.project13.scala.oculus.HadoopProcessRunner

object JobRunner extends App with OculusJobs {

  import pl.project13.scala.rainbow._

  val availableJobs =
    (0, "hash all files", hashAllSequenceFiles _) ::
    (1, "compare two movies", compareTwoMovies _) ::
    (2, "find movies similar to given", findSimilarToGiven _) ::
    Nil

  val availableJobsString = availableJobs.map(d => "  " + d._1 + ") " + d._2).mkString("\n")
  println("Available jobs to run: \n".bold + availableJobsString)
  println("Please run with [id] of task you want to execute.")

  val selected = availableJobs.drop(args(0).toInt).head

  val task = selected._3

  task(args)

}

trait OculusJobs {
  import pl.project13.scala.rainbow._

  private val conf = new Configuration(false)
  private val tool = new scalding.Tool

  /** Override if you need other than default settings - loads up ''application.conf'' */
  def appConfig: Config = ConfigFactory.load()

  conf.setStrings("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem")
  allOculusHadoopSettings(appConfig) foreach { case (key, value) =>
    conf.setStrings(key, value.unwrapped.toString)
  }


  def hashAllSequenceFiles(args: Seq[String]) = {
    val fs = FileSystem.get(conf)
    val seqFiles = fs.listStatus(new Path("hdfs:///oculus/source/")).map(_.getPath.toString)

    println(s"Found [${seqFiles.length}] sequence files. Will hash all of them.".green)

    val totalStopwatch = (new Stopwatch).start()

    val jobClass = classOf[HashVideoSeqFilesJob]
    val jobClassName = jobClass.getCanonicalName

    for(seq <- seqFiles) {
      println(s"Starting execution of jobs for $seq ...".green)
      val stopwatch = (new Stopwatch).start()

      val allArgs = Array(
        jobClassName,
        "--hdfs", IPs.HadoopMasterWithPort,
        "--input", seq
      )

      println("-----------------------------------".bold)
      println(("allArgs = " + allArgs.toList).bold)
      println(("cascading.app.appjar.class = " + jobClassName).bold)
      println("-----------------------------------".bold)

      Mode.mode = Hdfs(false, conf)

//      // todo oh my god, the pain!
//      conf.setClass("cascading.app.appjar.class", classOf[scalding.Tool], classOf[hadoop.util.Tool])
//      FixCascading.getApplicationJarClass(classOf[hadoop.util.Tool])
//
//      hadoop.util.ToolRunner.run(conf, tool, allArgs)

      HadoopProcessRunner(allArgs.toList).runAndWait()

      println(s"Finished running scalding job for [$seq}]. Took ${stopwatch.stop()}".green)
    }

    println(s"Finished running all jobs. Took ${totalStopwatch.stop()}".green)
  }


  def compareTwoMovies(args: Seq[String]) = {
    simpleHadoopRun(args, classOf[CompareTwoMoviesJob])
  }
  
  def findSimilarToGiven(args: Seq[String]) = {
    simpleHadoopRun(args, classOf[FindSimilarMoviesJob])
  }


  def simpleHadoopRun(args: Seq[String], jobClazz: Class[_]) {
    val totalStopwatch = (new Stopwatch).start()

    val jobClassName = jobClazz.getCanonicalName

    println(s"Starting execution of job $jobClassName ...".green)

    val allArgs = List(
      jobClassName,
      "--hdfs", IPs.HadoopMasterWithPort
    ) ++ args.toList

    println("-----------------------------------".bold)
    println(("allArgs = " + allArgs.toList).bold)
    println("-----------------------------------".bold)

    HadoopProcessRunner(allArgs.toList).runAndWait()

    println(s"Finished running all jobs. Took ${totalStopwatch.stop()}".green)
  }

  private def allOculusHadoopSettings(configuration: com.typesafe.config.Config) =
    configuration.getConfig("oculus").getConfig("hadoop").entrySet().toList.map(a => (a.getKey, a.getValue))
}