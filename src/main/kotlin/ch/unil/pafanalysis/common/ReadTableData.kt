package ch.unil.pafanalysis.common

import java.io.BufferedReader
import java.io.FileReader

class ReadTableData {

    fun getColumnNumbers(
        resultTablePath: String?,
        columnNames: List<String>,
        selColumns: List<String>,
        nanStrings: List<String>? = null
    ): List<List<Double>> {
        val reader = BufferedReader(FileReader(resultTablePath))

        // the first line is the header
        reader.readLine()

        val selIdx = columnNames.foldIndexed(emptyList<Int>()) { i, acc, col ->
            if (selColumns.contains(col)) acc + i else acc
        }

        return reader.readLines().fold(selIdx.map { emptyList<Double>() }) { acc, l ->
            val cols: List<String> = l.split("\t")
            val colVals: List<Double> = selIdx.map {
                if (nanStrings != null && nanStrings.contains(cols[it])) {
                    Double.NaN
                } else {
                    cols[it].toDouble()
                }
            }
            acc.mapIndexed { index, list -> list + colVals[index] }
        }
    }
}