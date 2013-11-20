package pl.project13.scala.oculus.hbase.tables

import java.lang.String
import org.joda.time.{DateMidnight, DateTime}
import com.gravity.hbase.schema._
import scala.collection._
import pl.project13.scala.oculus.hbase.HBaseConfig

/**
 * Key = phash
 */
object HashesSchema extends Schema with HBaseConfig {

  import com.gravity.hbase.schema

  class HashesTable extends HbaseTable[HashesTable, String, PropsTableRow]("hashes", new NoOpCache[HashesTable, String, PropsTableRow](), classOf[String])(hbaseConf, schema.StringConverter) {

    def rowBuilder(result: DeserializedResult) = new PropsTableRow(this, result)

    val youtube = family[String, String, Any]("youtube")

    /** can be used to look up movie in 'metadata' by key */
    val id = column(youtube, "id", classOf[String])

  }

  class PropsTableRow(table: HashesTable, result: DeserializedResult) extends HRow[HashesTable, String](result, table)

  val HashesTable = table(new HashesTable)

}