package fi.oph.suorituspalvelu.util

import java.io.{ByteArrayOutputStream, InputStream}
import java.util.zip.ZipInputStream

object ZipUtil {

  //Paluuarvo-map filename -> content
  def unzipStreamByFile(inputStream: InputStream, encoding: String = "UTF-8"): Map[String, String] = {
    val zipInputStream = new ZipInputStream(inputStream)
    try {
      val result = scala.collection.mutable.Map[String, String]()
      var entry = zipInputStream.getNextEntry
      while (entry != null) {
        if (!entry.isDirectory) {
          // Read the entry content
          val outputStream = new ByteArrayOutputStream()
          val buffer = new Array[Byte](4096)
          var len = zipInputStream.read(buffer)

          while (len > 0) {
            outputStream.write(buffer, 0, len)
            len = zipInputStream.read(buffer)
          }

          result(entry.getName) = new String(outputStream.toByteArray(), encoding)
          outputStream.close()
        }

        zipInputStream.closeEntry()
        entry = zipInputStream.getNextEntry
      }

      result.toMap
    } finally {
      zipInputStream.close()
    }
  }
}
