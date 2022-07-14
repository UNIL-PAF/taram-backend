package ch.unil.pafanalysis.common

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.ExpInfo
import ch.unil.pafanalysis.results.model.ResultType
import java.io.BufferedReader
import java.io.FileReader
import java.util.*

class ReadTableData {

    val checkTypes = CheckTypes()

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

    fun getTable(resultTablePath: String?): Table {
        val reader = BufferedReader(FileReader(resultTablePath))
        val headerNames: List<String> = reader.readLine().split("\t")
        val rowsStrings: List<List<String>> = reader.readLines().fold(mutableListOf()) { acc, r ->
            val line: List<String> = r.split("\t")
            acc.add(line)
            acc
        }

        val headerTypes: List<ColType> =
            rowsStrings.fold(Collections.nCopies(headerNames.size, ColType.NUMBER)) { acc, r ->
                r.mapIndexed { i, s ->
                    if ((checkTypes.isNumerical(s) || s.isEmpty()) && acc[i] == ColType.NUMBER) {
                        ColType.NUMBER
                    } else ColType.CHARACTER
                }
            }

        val headers = headerNames.mapIndexed{ i, name -> Header(name, i, headerTypes[i])}

        val rows: List<List<Any>> = rowsStrings.map { r ->
            r.mapIndexed { i, c ->
                if (headerTypes[i] == ColType.NUMBER) {
                    if (c.isNotEmpty()) c.toDouble() else Double.NaN
                } else c
            }
        }
        return Table(headers, rows)
    }

    data class Table(val headers: List<Header>, val rows: List<List<Any>>)

    data class Header(val name: String, val idx: Int, val type: ColType)

    enum class ColType(val value: String) {
        CHARACTER("character"),
        NUMBER("number")
    }
}