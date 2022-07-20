package ch.unil.pafanalysis.common

import ch.unil.pafanalysis.analysis.model.*
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

    fun getTable(resultTablePath: String?, columnInfo: ColumnInfo?): Table {
        val reader = BufferedReader(FileReader(resultTablePath))
        val headerOrigNames: List<String> = reader.readLine().split("\t")
        val headerNames = emptyList<String>()//parseNames(headerOrigNames, columnInfo)

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

        val nameAndSamples = parseNameAndSample(headerNames)

        //val headers =  //nameAndSamples.mapIndexed{ i, name -> Header(name.first, headerOrigNames[i], i, headerTypes[i], name.second)}

        val rows: List<List<Any>> = rowsStrings.map { r ->
            r.mapIndexed { i, c ->
                if (headerTypes[i] == ColType.NUMBER) {
                    if (c.isNotEmpty()) c.toDouble() else Double.NaN
                } else c
            }
        }
        return Table(emptyList(), rows)
    }


    fun parseNameAndSample(origNames: List<String>, resultType: ResultType? = null): List<Pair<String, Experiment?>>{
        return if(resultType == null){
            origNames.map{ Pair(it, null)}
        }else{
            when (resultType){
                ResultType.MaxQuant -> parseMaxQuant(origNames)
                ResultType.Spectronaut -> parseSpectronaut(origNames)
            }
        }
    }

    fun parseMaxQuant(origNames: List<String>): List<Pair<String, Experiment?>>{
        return emptyList()
    }

    fun parseSpectronaut(origNames: List<String>): List<Pair<String, Experiment?>>{
        val regex1 = Regex(".+_DIA_(\\d+?)_.+\\.(\\w+)$")
        val regex2 = Regex(".+(\\d+?)_DIA_.+\\.(\\w+)$")
        return origNames.map{ s ->
            val r = s.replace("PG.", "")
            val matchResult = regex1.matchEntire(r) ?: regex2.matchEntire(r)
            if (matchResult != null) {
                Pair(matchResult.groupValues[2], Experiment(matchResult.groupValues[1], matchResult.groupValues[2]))
            } else {
                Pair(r, null)
            }
        }
    }

}