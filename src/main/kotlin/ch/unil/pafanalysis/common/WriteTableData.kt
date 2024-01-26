package ch.unil.pafanalysis.common

import ch.unil.pafanalysis.analysis.model.ColType
import java.io.*

class WriteTableData {

    fun write(filename: String, table: Table): String {
        val writer = BufferedWriter(FileWriter(filename))
        val sep = "\t"
        writer.write(table.headers?.map{it.name}?.joinToString(separator = sep))
        writer.newLine()
        for(i in 0 until table.cols!![0].size){
            table.headers?.forEach { h ->
                when(h.type){
                    ColType.NUMBER -> writer.write((table.cols?.get(h.idx)[i] as? Double ?: Double.NaN).toString())
                    ColType.CHARACTER -> writer.write((table.cols?.get(h.idx)[i] as? String ?: ""))
                }
                if(h.idx == table.headers.size-1) writer.newLine()
                else writer.write(sep)
            }
        }
        writer.close()
        return filename
    }

}