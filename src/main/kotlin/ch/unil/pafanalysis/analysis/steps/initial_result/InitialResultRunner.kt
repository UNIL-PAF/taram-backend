package ch.unil.pafanalysis.analysis.steps.initial_result

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import ch.unil.pafanalysis.analysis.service.AnalysisStepRepository
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.results.model.Result
import ch.unil.pafanalysis.results.model.ResultType
import com.google.gson.Gson
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.sql.Timestamp
import java.time.LocalDateTime


@Service
class InitialResultRunner {

    @Autowired
    private var env: Environment? = null

    @Autowired
    private var analysisRepository: AnalysisRepository? = null

    @Autowired
    private var analysisStepRepository: AnalysisStepRepository? = null

    private val type = AnalysisStepType.INITIAL_RESULT.value
    private val gson = Gson()

    fun run(analysisId: Int?, result: Result?): String {
        val resultType = if (result?.type == ResultType.MaxQuant.value) ResultType.MaxQuant else ResultType.Spectronaut

        val outputRoot =
            env?.getProperty(if (resultType == ResultType.MaxQuant) "output.path.maxquant" else "output.path.spectronaut")
        val resultPath =
            env?.getProperty(if (resultType == ResultType.MaxQuant) "result.path.maxquant" else "result.path.spectronaut") + result?.path
        val outputPath: String = analysisId.toString()

        val analysis = if (analysisId == null) null else analysisRepository?.findById(analysisId)
        val newStep: AnalysisStep? = createEmptyAnalysisStep(analysis)

        createResultDir(outputRoot?.plus(outputPath))
        val stepPath = "$outputPath/${newStep?.id}"
        createResultDir(outputRoot?.plus(stepPath))
        val newTable = copyResultsTable(outputRoot?.plus(stepPath) + "/" + (result?.resFile), resultPath, resultType)

        val step: AnalysisStep? = try {
            val initialResult = createInitialResult(resultPath, result?.resFile, resultType)
            val columns = getColumns(resultPath + "/" + result?.resFile)
            val columnMapping = getColumnMapping(resultPath, columns, resultType)
            /*val columnMapping =
                ColumnMapping(experimentNames = experiments.second, experimentDetails = experiments.first, columns = columns)

             */

            newStep?.copy(
                resultPath = stepPath,
                resultTablePath = "$stepPath/${newTable?.name}",
                status = AnalysisStepStatus.DONE.value,
                results = gson.toJson(initialResult),
                columnMapping = columnMapping
            )
        } catch (e: StepException) {
            newStep?.copy(status = AnalysisStepStatus.ERROR.value, error = e.message)
        }

        if (step != null) {
            analysisStepRepository?.save(step)
            return "done"
        } else {
            throw RuntimeException("Could not create/save initial_result.")
        }
    }

    private fun createEmptyAnalysisStep(analysis: Analysis?): AnalysisStep? {
        val newStep = AnalysisStep(
            status = AnalysisStepStatus.IDLE.value,
            type = type,
            analysis = analysis,
            lastModifDate = LocalDateTime.now()
        )
        return analysisStepRepository?.save(newStep)
    }

    private fun createInitialResult(resultPath: String?, resultFilename: String?, type: ResultType): InitialResult {
        if (type == ResultType.MaxQuant) {
            return createInitialMaxQuantResult(resultPath, resultFilename)
        } else {
            return createInitialSpectronautResult(resultPath, resultFilename)
        }
    }

    private fun createInitialSpectronautResult(spectronautPath: String?, fileName: String?): InitialResult {
        return InitialResult(
            nrProteinGroups = getNrProteinGroups(spectronautPath.plus(fileName))
        )
    }

    private fun createInitialMaxQuantResult(maxQuantPath: String?, fileName: String?): InitialResult {
        return InitialResult(
            maxQuantParameters = parseMaxquantParameters(maxQuantPath.plus(fileName)),
            nrProteinGroups = getNrProteinGroups(maxQuantPath + "proteinGroups.txt")
        )
    }

    private fun copyResultsTable(outputPath: String?, resultPath: String, type: ResultType): File {
        val timestamp = Timestamp(System.currentTimeMillis())
        if (type == ResultType.MaxQuant) {
            return copyProteinGroupsTable(outputPath, resultPath, timestamp)
        } else {
            return copySpectronautResultTable(outputPath, resultPath, timestamp)
        }
    }

    private fun copySpectronautResultTable(outputPath: String?, spectronautPath: String?, timestamp: Timestamp): File {
        val originalTable = File(spectronautPath)
        return originalTable.copyTo(File(outputPath + "/Report_" + timestamp.time + ".txt"), overwrite = true)
    }

    private fun copyProteinGroupsTable(outputPath: String?, maxQuantPath: String?, timestamp: Timestamp): File {
        val originalTable = File(maxQuantPath + "proteinGroups.txt")
        return originalTable.copyTo(File(outputPath + "/proteinGroups_" + timestamp.time + ".txt"), overwrite = true)
    }

    private fun getNrProteinGroups(proteinGroupsTable: String): Int {
        val reader = BufferedReader(FileReader(proteinGroupsTable))
        var lines = 0
        while (reader.readLine() != null) lines++
        reader.close()
        lines--
        return lines
    }

    private fun parseMaxquantParameters(parametersTable: String): MaxQuantParameters {
        val paramsFile = File(parametersTable)
        if (!paramsFile.exists()) throw StepException("Could not find parameters.txt in the results directory.")
        val pMap: HashMap<String, String> = HashMap<String, String>()
        paramsFile.useLines { lines ->
            lines.forEach {
                val (n, v) = it.split("\t")
                pMap[n] = v
            }
        }

        val matchBetweenRuns = pMap["Match between runs"] == "True"
        return MaxQuantParameters(version = pMap["Version"], matchBetweenRuns = matchBetweenRuns)
    }

    private fun createResultDir(outputPath: String?): String {
        if (outputPath == null) throw RuntimeException("There is no output path defined.")
        val dir = File(outputPath)
        if (!dir.exists()) dir.mkdir()
        return outputPath
    }

    private fun getColumns(filePath: String?): List<String> {
        val header: String = File(filePath).bufferedReader().readLine()
        return header.split("\t")
    }

    private fun getColumnMapping(resultPath: String, columns: List<String>, type: ResultType): ColumnMapping {
        if (type == ResultType.MaxQuant) {
            return getMaxQuantExperiments(columns, resultPath.plus("summary.txt"))
        } else {
            return getSpectronautExperiments(columns)
        }
    }

    private fun parseExperimentalColumns(columns: List<String>, oneOrigName: String?): List<String> {
        return columns.fold(emptyList<String>()) { sum, col ->
            if (col.contains(oneOrigName!!)) {
                sum.plus(col.replace(oneOrigName, "").trim())
            } else {
                sum
            }
        }
    }

    private fun getSpectronautExperiments(columns: List<String>): ColumnMapping {
        val quantRegex = Regex(".+DIA_(.+?)_.+\\.Quantity$")

        val quantityCols: List<Pair<String, Int>> = columns.foldIndexed(emptyList()) { index, sum, col ->
            val matchResult = quantRegex.matchEntire(col)
            if (matchResult != null) {
                sum.plus(Pair(matchResult.groupValues[1], index))
            } else {
                sum
            }
        }

        if (quantityCols.isEmpty()) throw StepException("Could not parse experiment names from columns.")

        val experimentDetails = quantityCols.map { col ->
            val originalName = columns[col.second].replace("Quantity", "")
            val expInfo = ExpInfo(isSelected = true, originalName = originalName, name = col.first)
            col.first to expInfo
        }.toMap()

        val experimentNames = quantityCols.map { it.first }
        val experimentalColumns = parseExperimentalColumns(columns, experimentDetails[experimentNames[0]]?.originalName)

        return ColumnMapping(
            columns = columns,
            intColumn = "Quantity",
            experimentColumns = experimentalColumns,
            experimentNames = experimentNames,
            experimentDetails = experimentDetails as HashMap
        )
    }

    private fun getMaxQuantExperiments(columns: List<String>, summaryTable: String): ColumnMapping {
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
