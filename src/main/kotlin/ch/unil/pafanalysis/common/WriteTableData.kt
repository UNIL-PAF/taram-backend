package ch.unil.pafanalysis.common

import ch.unil.pafanalysis.analysis.model.AnalysisStep
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

    fun writeTable(
        step: AnalysisStep?,
        newInts: List<List<Double>>,
        outputRoot: String?,
        colName: String
    ): String {
        val origTable = outputRoot + step?.resultTablePath
        val newTable = "$origTable.tmp"

        val reader = BufferedReader(FileReader(origTable))
        val writer = BufferedWriter(FileWriter(newTable))

        val oldHeader = reader.readLine().split("\t")
        val newNames = newInts.map { (name, ints) -> "$colName $name" }
        val newHeader = oldHeader.plus(newNames)
        writer.write(newHeader.joinToString(separator = "\t"))
        writer.newLine()

        val initialList: List<List<Double>> = newInts[0].map { emptyList<Double>() }
        val intMatrix: List<List<Double>> = newInts.fold(initialList) { sum, ints ->
            ints.mapIndexed { i, int ->
                sum[i].plus(int)
            }
        }

        intMatrix.forEach { pg ->
            val origCols = reader.readLine().split("\t")
            writer.write(origCols.plus(pg).joinToString(separator = "\t"))
            writer.newLine()
        }

        writer.close()
        reader.close()

        File(origTable).delete()
        File(newTable).let { sourceFile ->
            sourceFile.copyTo(File(origTable))
            sourceFile.delete()
        }

        return origTable
    }
}