package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.steps.CommonResult
import ch.unil.pafanalysis.common.CheckTypes
import ch.unil.pafanalysis.results.model.ResultType
import org.springframework.stereotype.Service
import java.io.File
import java.util.*

@Service
class ColumnMappingParser {

    val checkTypes = CheckTypes()

    fun parse(filePath: String?, resultPath: String?, resultType: ResultType?): Pair<ColumnMapping, CommonResult> {
        val (columns, colTypes) = getColumns(filePath)
        return getColumnMapping(resultPath, columns, resultType, colTypes)
    }

    private fun getColumns(filePath: String?): Pair<List<String>, List<ColType>> {
        val reader = File(filePath).bufferedReader()
        val headerNames: List<String> = reader.readLine().split("\t")

        val headerTypes: List<ColType> =
            reader.readLines().fold(Collections.nCopies(headerNames.size, ColType.NUMBER)) { acc, r ->
                val c = r.split("\t")
                c.mapIndexed { i, s ->
                    if ((checkTypes.isNumerical(s) || s.isEmpty() || s == "Filtered") && acc[i] == ColType.NUMBER) {
                        ColType.NUMBER
                    } else ColType.CHARACTER
                }
            }

        return Pair(headerNames, headerTypes)
    }

    private fun getColumnMapping(
        resultPath: String?,
        columns: List<String>?,
        type: ResultType?,
        colTypes: List<ColType>?
    ): Pair<ColumnMapping, CommonResult> {
        return if (type == ResultType.MaxQuant) {
            getMaxQuantExperiments(columns, resultPath.plus("summary.txt"), colTypes)
        } else {
            getSpectronautExperiments(columns, colTypes)
        }
    }

    data class ColumnsParsed(
        val expNames: Set<String> = emptySet(),
        val expFields: Set<String> = emptySet(),
        val expDetails: Map<String, ExpInfo> = emptyMap(),
        val headers: List<Header> = emptyList()
    )

    private fun getSpectronautExperiments(
        columns: List<String>?,
        colTypes: List<ColType>?
    ): Pair<ColumnMapping, CommonResult> {

        val regex1 = Regex(".+_DIA_(\\d+?)_.+\\.(.+?)$")
        val regex2 = Regex(".+_(\\d+?)_DIA_.+\\.(.+?)$")
        val regex3 = Regex(".+_(\\d+?)_\\d+min_DIA_.+\\.(.+?)$")

        val cols: ColumnsParsed = columns!!.foldIndexed(ColumnsParsed()) { i, acc, s ->
            val matchResult = regex1.matchEntire(s) ?: regex2.matchEntire(s) ?: regex3.matchEntire(s)
            val accWithExp = if (matchResult != null) {
                acc.copy(
                    expNames = acc.expNames.plus(matchResult.groupValues[1]),
                    expFields = acc.expFields.plus(matchResult.groupValues[2]),
                    headers = acc.headers.plus(
                        Header(
                            name = matchResult.groupValues[1] + "." + matchResult.groupValues[2],
                            idx = i,
                            type = colTypes?.get(i),
                            experiment = Experiment(name = matchResult.groupValues[1], field = matchResult.groupValues[2], initialName = matchResult.groupValues[1])
                        )
                    ),
                    expDetails = acc.expDetails.plus(
                        Pair(
                            matchResult.groupValues[1],
                            ExpInfo(
                                isSelected = true,
                                name = matchResult.groupValues[1],
                                originalName = s.replace(matchResult.groupValues[2], "")
                            )
                        )
                    )
                )
            } else acc.copy(headers = acc.headers.plus(Header(name = s, idx = i, type = colTypes?.get(i))))
            accWithExp
        }

        val colMapping = ColumnMapping(
            experimentDetails = cols.expDetails,
            experimentNames = cols.expNames.toList(),
            intCol = if (cols.expFields.contains("Quantity")) "Quantity" else null
        )

        val commonResult = CommonResult(
            numericalColumns = cols.headers.filterIndexed{i, c ->
                colTypes?.get(i) == ColType.NUMBER  && c.experiment != null}.map{it.experiment!!.field}.distinct(),
            headers = cols.headers
        )
        return Pair(colMapping, commonResult)
    }

    private fun getMaxQuantExperiments(
        columns: List<String>?,
        summaryTable: String,
        colTypes: List<ColType>?
    ): Pair<ColumnMapping, CommonResult> {
        val expsParsed = parseMaxQuantExperiments(summaryTable)

        val cols = columns?.foldIndexed(expsParsed){ i, acc, col ->
            val expName: String? = acc.expNames.find{ col.contains(it) }
            if(expName != null){
                val field = col.replace(expName, "").trim().replace(Regex("[^A-Za-z0-9]+"), ".").replace(Regex("^\\.*|\\.*\$"), "")
                val expFields = acc.expFields.plus(field)
                val headers = acc.headers.plus(Header(name = "$expName.$field", idx = i, type = colTypes?.get(i), experiment = Experiment(expName, field, initialName = expName)))
                acc.copy(expFields = expFields, headers = headers)
            }else{
                val field = col.trim().replace(Regex("[^A-Za-z0-9]+"), ".").replace(Regex("^\\.*|\\.*\$"), "")
                acc.copy(headers = acc.headers.plus(Header(name = field, idx = i, type = colTypes?.get(i))))
            }

        }

        val colMapping = ColumnMapping(
            experimentDetails = cols?.expDetails,
            experimentNames = cols?.expNames?.toList(),
            intCol = if (cols?.expFields?.contains("Intensity") == true) "Intensity" else null
        )

        val commonResult = CommonResult(
            numericalColumns = cols?.headers?.filterIndexed{i, c ->
                colTypes?.get(i) == ColType.NUMBER  && c.experiment != null}?.map{it.experiment!!.field}?.distinct(),
            headers = cols?.headers
        )
        return Pair(colMapping, commonResult)
    }

    private fun parseMaxQuantExperiments(summaryTable: String): ColumnsParsed{
        val lines: List<String> = File(summaryTable).bufferedReader().readLines()
        val headers: List<String> = lines[0].split("\t")
        val expIdx = headers.indexOf("Experiment")
        val fileIdx = headers.indexOf("Raw file")

        return lines.subList(1, lines.size - 1)
            .fold(ColumnsParsed()) { sum, el ->
                val l = el.split("\t")
                val expName = l[expIdx]
                val fileName = if(fileIdx > -1) l[fileIdx] else null
                val expInfo = ExpInfo(fileName = fileName, isSelected = true, name = expName, originalName = expName)
                sum.copy(expNames = sum.expNames.plus(expName), expDetails = sum.expDetails.plus(Pair(expName, expInfo)))
            }
    }
}