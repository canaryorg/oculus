package pl.project13.scala.oculus.job

import com.twitter.scalding._
import pl.project13.scala.oculus.IPs
import pl.project13.scala.scalding.hbase.MyHBaseSource
import org.apache.commons.io.FilenameUtils
import org.apache.hadoop.io.{BytesWritable, IntWritable}
import pl.project13.scala.oculus.conversions.WriteDOT
import pl.project13.scala.oculus.source.hbase.HistogramsSource

class HistogramSeqFilesJob(args: Args) extends Job(args)
  with WriteDOT
  with HistogramsSource
  with Histograms {

  val input = args("input")

  implicit val mode = Read

  val youtubeId = FilenameUtils.getBaseName(input)

  WritableSequenceFile(input, ('key, 'value))
    .read
    .rename('key, 'frame)
    .map(('frame, 'value) -> ('id, 'lumHist)) { p: (Int, BytesWritable) =>
      val histogram = mkHistogram(p)
      val luminance = histogram.getLuminanceHistogram
      val lumString = luminance.map(Integer.toHexString).mkString

      youtubeId.asImmutableBytesWriteable -> lumString.asImmutableBytesWriteable
    }
    .map('frame -> 'frame) { p: IntWritable => longToIbw(p.get) }
    .write(HistogramsTable)

}

