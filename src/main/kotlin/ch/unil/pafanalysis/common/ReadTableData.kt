package ch.unil.pafanalysis.common

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.results.model.ResultType
import java.io.BufferedReader
import java.io.FileReader
import java.util.*

class ReadTableData {

    fun getListOfInts(
        expInfoList: List<ExpInfo?>?,
        analysisStep: AnalysisStep?,
        outputRoot: String?,
        intColumn: String? = null
    ): List<Pair<String, List<Double>>> {
        val intColumn = intColumn ?: analysisStep?.commonResult?.intCol

        val intColumnNames = expInfoList?.map { exp ->
            if (analysisStep?.analysis?.result?.type == ResultType.MaxQuant.value) {
                intColumn + " " + exp?.originalName
            } else exp?.originalName + intColumn
        }

        val filterTerms: List<String>? = if (analysisStep?.analysis?.result?.type == ResultType.Spectronaut.value) {
            listOf("Filtered")
        } else null

        val columnInts: List<List<Double>> = ReadTableData().getColumnNumbers(
            outputRoot?.plus(analysisStep?.resultTablePath),
            intColumnNames!!,
            filterTerms
        )

        return columnInts.mapIndexed { i, ints -> Pair(expInfoList[i]!!.name!!, ints) }
    }

    fun getColumnNumbers(
        resultTablePath: String?,
        selColumns: List<String>,
        nanStrings: List<String>? = null
    ): List<List<Double>> {
        val reader = BufferedReader(FileReader(resultTablePath))

        // the first line is the header
        val header = reader.readLine().split("\t")

        val selIdx = header.foldIndexed(emptyList<Int>()) { i, acc, col ->
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

    fun getTable(resultTablePath: String?, columnMapping: ColumnMapping?): Table {
        val reader = BufferedReader(FileReader(resultTablePath))
        // ignore the first line, it is the header
        reader.readLine()

        val rowsStrings: List<List<String>> = reader.readLines().fold(mutableListOf()) { acc, r ->
            val line: List<String> = r.split("\t")
            acc.add(line)
            acc
        }

        val cols: List<List<Any>> = rowsStrings.fold(Collections.nCopies(columnMapping!!.headers!!.size, emptyList<Any>())) { acc, r ->
            r.mapIndexed { i, c ->
                val colVal = if (columnMapping?.headers?.get(i)?.type == ColType.NUMBER) {
                    if (c.isNotEmpty() && c != "Filtered") c.toDouble() else Double.NaN
                } else c
                acc[i].plus(colVal)
            }
        }
        return Table(columnMapping?.headers, cols)
    }

    fun getDoubleMatrix(table: Table?, field: String?, group: String? = null): Pair<List<Header>, List<List<Double>>> {
        val headers = table?.headers?.filter { it.experiment?.field == field && (group == null || it.experiment?.group == group)}
        if(headers.isNullOrEmpty()) throw Exception("No entries for [$field] found.")
        if(! headers.all{it.type == ColType.NUMBER}) throw Exception("Entries for [$field] are not numerical.")

        return Pair(headers, headers.map{ h -> table!!.cols!![h.idx].map { it as? Double ?: Double.NaN }})
    }


    fun getDoubleColumn(table: Table?, headerName: String): List<Double>? {
        val header = table?.headers?.find { it.name == headerName } ?: return null
        if(header.type != ColType.NUMBER) throw Exception("Cannot extract double from non-numeric column.")
        return table?.cols?.get(header.idx)?.map { it as? Double ?: Double.NaN }
    }

    fun getStringColumn(table: Table?, headerName: String): List<String>? {
        val header = table?.headers?.find { it.name == headerName } ?: return null
        if(header.type != ColType.CHARACTER) throw Exception("Cannot extract string from non-character column.")
        return table?.cols?.get(header.idx)?.map { it as? String ?: "" }
    }

}