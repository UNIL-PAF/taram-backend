package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.steps.CommonResult
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.analysis.steps.initial_result.spectronaut.ParseSpectronautColNames
import ch.unil.pafanalysis.common.CheckTypes
import ch.unil.pafanalysis.results.model.ResultType
import org.springframework.stereotype.Service
import java.io.File
import java.util.*

@Service
class ColumnMappingParser {

    val customTypeMatch: List<Pair<Regex, ColType>> = listOf(Regex(".*OrganismId") to ColType.CHARACTER)

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

        val headerTypesWithCustom = headerNames.mapIndexed { i, s ->
            val typeMatch: Pair<Regex, ColType>? = customTypeMatch.find{ a -> a.first.matches(s)}
            typeMatch?.second ?: headerTypes[i]
        }

        return Pair(headerNames, headerTypesWithCustom)
    }

    private fun getColumnMapping(
        resultPath: String?,
        columns: List<String>?,
        type: ResultType?,
        colTypes: List<ColType>?
    ): Pair<ColumnMapping, CommonResult> {
        return if (type == ResultType.MaxQuant) {
            val summaryFile = resultPath.plus("summary.txt")
            if (! File(summaryFile).exists()) throw StepException("Could not find summary.txt in results directory.")
            getMaxQuantExperiments(columns, summaryFile, colTypes)
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
        val cols = parseSpectronautColumns(columns, colTypes)

        if(cols.expNames.isEmpty()) throw StepException("Could not parse column names from spectronaut result.")

        val colMapping = ColumnMapping(
            experimentDetails = cols.expDetails,
            experimentNames = cols.expNames.toList(),
            intCol = if (cols.expFields.contains("Quantity")) "Quantity" else null
        )

        val commonResult = CommonResult(
            headers = cols.headers
        )
        return Pair(colMapping, commonResult)
    }

    private fun parseSpectronautColumns(columnsOrig: List<String>?, colTypes: List<ColType>?):ColumnsParsed {
        // Remove trailing " (Settings)"
        val columns = columnsOrig?.map{it.replace(Regex("\\s\\(Settings\\)$"), "")}
        val allEndings = columns?.map{c -> c.split(".").last()}

        val endingSizes = allEndings?.fold(emptyMap<String, Int>()){ a, v ->
            a + (v to (a[v]?.plus(1) ?: 1))
        }

        val selField = endingSizes?.toList()?.maxByOrNull { it.second }!!.first
        val selCols = columns?.filter{it.matches(Regex(".+${selField}$"))}
        val (commonStart, commonEnd) = ParseSpectronautColNames.getCommonStartAndEnd(selCols, selField)
        val dynRegex = Regex("^.*?_${commonStart}(.+)${commonEnd}.+")

        return columns!!.foldIndexed(ColumnsParsed()) { i, acc, s ->
            val matchResult = if(dynRegex.matches(s)){
                val myMatch = dynRegex.matchEntire(s)
                if(myMatch != null){
                    val a = myMatch.groupValues[1]
                    // remove all "min" and "DIA"
                    val b = a.replace(Regex("_\\d+min_.*"), "")
                    val c = b.replace(Regex("_DIA.*"), "")
                    val d = c.replace(Regex("^_"), "")
                    d
                } else null
            } else null

            val colField = s.split(".").last()

            val accWithExp = if (matchResult != null) {
                acc.copy(
                    expNames = acc.expNames.plus(matchResult),
                    expFields = acc.expFields.plus(colField),
                    headers = acc.headers.plus(
                        Header(
                            name = matchResult + "." + colField,
                            idx = i,
                            type = colTypes?.get(i),
                            experiment = Experiment(name = matchResult, field = colField)
                        )
                    ),
                    expDetails = acc.expDetails.plus(
                        Pair(
                            matchResult,
                            ExpInfo(
                                isSelected = true,
                                name = matchResult,
                                originalName = s.replace(colField, "")
                            )
                        )
                    )
                )
            } else acc.copy(headers = acc.headers.plus(Header(name = s, idx = i, type = colTypes?.get(i))))
            accWithExp
        }
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
                val headers = acc.headers.plus(Header(name = "$expName.$field", idx = i, type = colTypes?.get(i), experiment = Experiment(expName, field)))
                acc.copy(expFields = expFields, headers = headers)
            }else{
                val field = col.trim().replace(Regex("[^A-Za-z0-9]+"), ".").replace(Regex("^\\.*|\\.*\$"), "")
                acc.copy(headers = acc.headers.plus(Header(name = field, idx = i, type = colTypes?.get(i))))
            }

        }

        // sort experimentNames
        val oneField = cols?.expFields?.first()
        val selHeaders = cols?.headers?.filter{h -> h.experiment?.field == oneField}
        val expNames = selHeaders?.map{ h -> h.experiment?.name ?: throw StepException("Could find experiment name in [${h.name}].") }
        if(expNames?.size != cols?.expNames?.size) throw StepException("Could not find all experiment names in header [$oneField].")

        val colMapping = ColumnMapping(
            experimentDetails = cols?.expDetails,
            experimentNames = expNames, //cols?.expNames?.toList(),
            intCol = if (cols?.expFields?.contains("iBAQ") == true) "iBAQ" else null
        )

        val commonResult = CommonResult(
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