package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.Crc32HashComputations
import ch.unil.pafanalysis.results.model.ResultType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.util.zip.CRC32
import java.util.zip.Checksum


@Transactional
@Service
class ColumnInfoService {

    @Autowired
    private var columnInfoRepository: ColumnInfoRepository? = null

    fun createAndSaveColumnInfo(filePath: String?, resultPath: String, type: ResultType): ColumnInfo ? {
        val columnInfo = createColumnInfo(filePath, resultPath, type)
        return columnInfoRepository?.save(columnInfo)
    }

    fun createColumnInfo(filePath: String?, resultPath: String, type: ResultType): ColumnInfo {
        val columns = getColumns(filePath)
        val columnMapping = getColumnMapping(resultPath, columns, type)
        val crc32Hash = Crc32HashComputations().computeCrc32Hash(columnMapping.toString())
        return ColumnInfo(columnMapping = columnMapping, crc32hash = crc32Hash)
    }

    private fun getColumns(filePath: String?): List<String> {
        val header: String = File(filePath).bufferedReader().readLine()
        return header.split("\t")
    }

    private fun getColumnMapping(resultPath: String, columns: List<String>?, type: ResultType): ColumnMapping {
        if (type == ResultType.MaxQuant) {
            return getMaxQuantExperiments(columns, resultPath.plus("summary.txt"))
        } else {
            return getSpectronautExperiments(columns)
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

    private fun getSpectronautExperiments(columns: List<String>?): ColumnMapping {
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

        return ColumnMapping(
            columns = columns,
            intColumn = "Quantity",
            experimentColumns = experimentalColumns,
            experimentNames = experimentNames,
            experimentDetails = experimentDetails as HashMap
        )
    }

    private fun getMaxQuantExperiments(columns: List<String>?, summaryTable: String): ColumnMapping {
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

        val experimentalColumns = parseExperimentalColumns(columns, experiments.first[experiments.second[0]]?.originalName)

        return ColumnMapping(
            columns = columns,
            intColumn = "Intensity",
            experimentNames = experiments.second,
            experimentColumns = experimentalColumns,
            experimentDetails = experiments.first
        )
    }
}