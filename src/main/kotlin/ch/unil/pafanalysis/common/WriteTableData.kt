package ch.unil.pafanalysis.common

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import java.io.*

class WriteTableData {

    fun writeTable(
        step: AnalysisStep?,
        newInts: List<Pair<String, List<Double>>>,
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

        val initialList: List<List<Double>> = newInts[0].second.map { emptyList<Double>() }
        val intMatrix: List<List<Double>> = newInts.fold(initialList) { sum, (name, ints) ->
            val myInts: List<Double> = ints
            myInts.mapIndexed { i, int ->
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