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
        val intColumn = intColumn ?: analysisStep?.columnInfo?.columnMapping?.intCol

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

    fun getRows(resultTablePath: String?, headers: List<Header>?): List<List<Any>>? {
        val reader = BufferedReader(FileReader(resultTablePath))
        // ignore the first line, it is the header
        reader.readLine()

        val rowsStrings: List<List<String>> = reader.readLines().fold(mutableListOf()) { acc, r ->
            val line: List<String> = r.split("\t")
            acc.add(line)
            acc
        }

        return rowsStrings.map{ row -> row.mapIndexed{ i, col ->
            if (headers?.get(i)?.type == ColType.NUMBER) {
                if (col.isNotEmpty() && col != "Filtered") col.toDouble() else Double.NaN
            } else col
        }}
    }

    fun getTableWithoutHeaders(resultTablePath: String?): Table? {
        val sep = "\t"
        val reader = BufferedReader(FileReader(resultTablePath))
        val headers = reader.readLine().split(sep).mapIndexed{ i, h -> Header(name = h, idx = i)}

        val rowsStrings: List<List<String>> = reader.readLines().fold(mutableListOf()) { acc, r ->
            val line: List<String> = r.split(sep)
            acc.add(line)
            acc
        }

        val table = rowsStrings.fold(Table(cols = Collections.nCopies(headers!!.size, emptyList<Any>()), headers = headers)){ acc, row ->
            val newCols = row.mapIndexed{ i, col ->
                acc.cols?.get(i)?.plusElement(col)!!
            }
            val newHeaders = acc.headers?.mapIndexed { i, header ->
                val col = row[i]
                if(col.isEmpty() || header?.type == ColType.CHARACTER) header
                else if(col.toDoubleOrNull() != null) header?.copy(type = ColType.NUMBER)
                else header?.copy(type = ColType.CHARACTER)
            }
            Table(cols = newCols, headers = newHeaders)
        }

        val mappedCols = table.cols?.mapIndexed{ i, col ->
            col.map{ a ->
                val c = a as String
                if (table.headers?.get(i)?.type == ColType.NUMBER) {
                    if (c.isNotEmpty() && c != "Filtered") c.toDouble() else Double.NaN
                } else c
            }
        }

        return table.copy(cols = mappedCols)
    }

    fun getTable(resultTablePath: String?, headers: List<Header>?): Table {
        val reader = BufferedReader(FileReader(resultTablePath))
        // ignore the first line, it is the header
        reader.readLine()

        val rowsStrings: List<List<String>> = reader.readLines().fold(mutableListOf()) { acc, r ->
            val line: List<String> = r.split("\t")
            acc.add(line)
            acc
        }

        val cols: List<List<Any>> = rowsStrings.fold(Collections.nCopies(headers!!.size, emptyList<Any>())) { acc, r ->
            r.mapIndexed { i, c ->
                val colVal = if (headers?.get(i)?.type == ColType.NUMBER) {
                    if (c.isNotEmpty() && c != "Filtered") c.toDouble() else Double.NaN
                } else c
                acc[i].plus(colVal)
            }
        }
        return Table(headers, cols)
    }

    fun getDoubleMatrix(table: Table?, field: String?, expDetails: Map<String, ExpInfo>? = null, group: String? = null): Pair<List<Header>, List<List<Double>>> {
        val headers = table?.headers?.filter { it.experiment?.field == field && (group == null || expDetails?.get(it.experiment?.name)?.group == group)}
        if(headers.isNullOrEmpty()) throw Exception("No entries for [$field] found.")
        if(! headers.all{it.type == ColType.NUMBER}) throw Exception("Entries for [$field] are not numerical.")
        return Pair(headers, headers.map{ h -> table!!.cols!![h.idx].map { it as? Double ?: Double.NaN }})
    }

    fun getDoubleMatrix(table: Table?, headers: List<Header>): List<List<Double>> {
        if(headers.isEmpty()) throw Exception("No headers...")
        if(! headers.all{it.type == ColType.NUMBER}) throw Exception("All headers have to be numerical.")
        return headers.map{ h -> table!!.cols!![h.idx].map { it as? Double ?: Double.NaN }}
    }

    fun getDoubleMatrixByRow(table: Table?, headers: List<Header>): List<List<Double>> {
        val matrix = getDoubleMatrix(table, headers)
        val initialList: List<List<Double>> = emptyList()
        val rowNr = matrix[0].size - 1
        return (0..rowNr).fold(initialList){ acc, i ->
            val row: List<Double> = matrix.map{it[i]}
            acc.plusElement(row)
        }
    }

    fun getStringMatrix(table: Table?, headers: List<Header>): List<List<String>> {
        if(headers.isEmpty()) throw Exception("No headers...")
        if(! headers.all{it.type == ColType.CHARACTER}) throw Exception("All headers have to be characters.")
        return headers.map{ h -> table!!.cols!![h.idx].map { it as? String ?: "" }}
    }

    fun getStringMatrixByRow(table: Table?, headers: List<Header>): List<List<String>> {
        val matrix = getStringMatrix(table, headers)
        val initialList: List<List<String>> = emptyList()
        val rowNr = matrix[0].size - 1
        return (0..rowNr).fold(initialList){ acc, i ->
            val row: List<String> = matrix.map{it[i]}
            acc.plusElement(row)
        }
    }

    fun getDoubleMatrixByRow(table: Table?, field: String?, expDetails: Map<String, ExpInfo>?, group: String? = null): Pair<List<Header>, List<List<Double>>> {
        val matrix = getDoubleMatrix(table, field, expDetails, group)
        val initialList: List<List<Double>> = emptyList()
        val rowNr = matrix.second[0].size - 1
        val byRow = (0..rowNr).fold(initialList){ acc, i ->
            val row: List<Double> = matrix.second.map{it[i]}
            acc.plusElement(row)
        }
        return Pair(matrix.first, byRow)
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

    fun getStringColumn(table: Table?, headerIdx: Int): List<String>? {
        val header = table?.headers?.find { it.idx == headerIdx } ?: return null
        if(header.type != ColType.CHARACTER) throw Exception("Cannot extract string from non-character column.")
        return table?.cols?.get(header.idx)?.map { it as? String ?: "" }
    }

}