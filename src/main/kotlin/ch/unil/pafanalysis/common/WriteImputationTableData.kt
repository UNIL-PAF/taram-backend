package ch.unil.pafanalysis.common

import java.io.BufferedWriter
import java.io.FileWriter

class WriteImputationTableData {

    fun write(filename: String, table: ImputationTable): String {
        val writer = BufferedWriter(FileWriter(filename))
        val sep = "\t"

        writer.write(table.headers?.map { it.idx }?.joinToString(separator = sep))
        writer.newLine()
        table.rows?.forEach{ row ->
            writer.write(row.map{ if(it == true) 1 else 0 }.joinToString(separator = sep))
            writer.newLine()
        }
        writer.close()
        return filename
    }

}