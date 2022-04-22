package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.ColumnInfo
import ch.unil.pafanalysis.analysis.model.ColumnMapping
import ch.unil.pafanalysis.analysis.model.ExpInfo
import ch.unil.pafanalysis.analysis.steps.CommonResult
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.CheckTypes
import ch.unil.pafanalysis.common.Crc32HashComputations
import ch.unil.pafanalysis.results.model.ResultType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File


@Transactional
@Service
class ColumnInfoService {

    @Autowired
    private var columnInfoRepository: ColumnInfoRepository? = null

    fun createAndSaveColumnInfo(filePath: String?, resultPath: String?, type: ResultType?): Pair<ColumnInfo?, CommonResult> ? {
        val (columnInfo, commonResult) = createColumnInfo(filePath, resultPath, type)
        return Pair(columnInfoRepository?.save(columnInfo), commonResult)
    }

    fun createColumnInfo(filePath: String?, resultPath: String?, type: ResultType?): Pair<ColumnInfo, CommonResult> {
        val (columns, isNumerical) = getColumns(filePath)
        val (columnMapping, commonResult) = getColumnMapping(resultPath, columns, type, isNumerical)
        val crc32Hash = Crc32HashComputations().computeStringHash(columnMapping.toString())
        return Pair(ColumnInfo(columnMapping = columnMapping, columnMappingHash = crc32Hash), commonResult)
    }

    private fun getColumns(filePath: String?): Pair<List<String>, List<Boolean>> {
        val reader = File(filePath).bufferedReader()
        val header: String = reader.readLine()

        val firstLine: String = reader.readLine()
        val isNumerical: List<Boolean> = firstLine.split("\t").map { v -> CheckTypes().isNumerical(v) }

        return Pair(header.split("\t"), isNumerical)
    }

    private fun getColumnMapping(
        resultPath: String?,
        columns: List<String>?,
        type: ResultType?,
        isNumerical: List<Boolean>?
    ): Pair<ColumnMapping, CommonResult> {
        if (type == ResultType.MaxQuant) {
            return getMaxQuantExperiments(columns, resultPath.plus("summary.txt"), isNumerical)
        } else {
            return getSpectronautExperiments(columns, isNumerical)
        }
    }

    private fun parseExperimentalColumns(columns: List<String>?, oneOrigName: String?): List<String>? {
        return columns?.fold(emptyList<String>()) { sum, col ->
            if (col.contains(oneOrigName!!)) {
                sum.plus(col.replace(oneOrigName, "").trim())
            } else {
                sum
            }
        }
    }

    private fun parseNumericalColumns(
        columns: List<String>?,
        expCols: List<String>?,
        isNumerical: List<Boolean>?
    ): List<String>? {
        return expCols?.filter { exp ->
            val validCols: List<String>? = columns?.filterIndexed { i, col ->
                col.contains(exp) && isNumerical?.get(i)!!
            }
            validCols?.size!! >= 1
        }
    }

    private fun getSpectronautExperiments(columns: List<String>?, isNumerical: List<Boolean>?): Pair<ColumnMapping, CommonResult> {
        val quantRegex = Regex(".+DIA_(.+?)_.+\\.Quantity$")

        val quantityCols: List<Pair<String, Int>>? = columns?.foldIndexed(emptyList()) { index, sum, col ->
            val matchResult = quantRegex.matchEntire(col)
            if (matchResult != null) {
                sum.plus(Pair(matchResult.groupValues[1], index))
            } else {
                sum
            }
        }

        if (quantityCols == null || quantityCols.isEmpty()) throw StepException("Could not parse experiment names from columns.")

        val experimentDetails = quantityCols?.map { col ->
            val originalName = columns[col.second].replace("Quantity", "")
            val expInfo = ExpInfo(isSelected = true, originalName = originalName, name = col.first)
            col.first to expInfo
        }?.toMap()

        val experimentNames = quantityCols?.map { it.first }
        val experimentalColumns = parseExperimentalColumns(columns, experimentDetails[experimentNames[0]]?.originalName)
        val numericalColumns = parseNumericalColumns(columns, experimentalColumns, isNumerical)

        val colMapping = ColumnMapping(
            numericalColumns = numericalColumns,
            experimentNames = experimentNames,
            experimentDetails = experimentDetails as HashMap
        )

        val commonResult = CommonResult(
            intCol = "Quantity",
            numericalColumns = numericalColumns
        )

        return Pair(colMapping, commonResult)
    }

    private fun getMaxQuantExperiments(
        columns: List<String>?,
        summaryTable: String,
        isNumerical: List<Boolean>?
    ): Pair<ColumnMapping, CommonResult> {
        val lines: List<String> = File(summaryTable).bufferedReader().readLines()
        val headers: List<String> = lines[0].split("\t")
        val expIdx = headers.indexOf("Experiment")
        val fileIdx = headers.indexOf("Raw file")

        val experiments = lines.subList(1, lines.size - 1)
            .fold(Pair(HashMap<String, ExpInfo>(), mutableListOf<String>())) { sum, el ->
                val l = el.split("\t")
                val expName = l[expIdx]
                val expInfo = ExpInfo(fileName = l[fileIdx], isSelected = true, name = expName, originalName = expName)
                if (!sum.first.containsKey(expName)) {
                    sum.first[expName] = expInfo
                    sum.second.add(expName)
                }
                sum
            }

        val experimentalColumns =
            parseExperimentalColumns(columns, experiments.first[experiments.second[0]]?.originalName)
        val numericalColumns = parseNumericalColumns(columns, experimentalColumns, isNumerical)

        val colMapping =  ColumnMapping(
            experimentNames = experiments.second,
            experimentDetails = experiments.first
        )

        val commonResult = CommonResult(
            intCol = "Intensity",
            numericalColumns = numericalColumns
        )

        return Pair(colMapping, commonResult)
    }
}