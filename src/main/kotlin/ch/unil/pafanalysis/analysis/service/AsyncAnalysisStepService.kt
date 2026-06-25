package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.steps.correlation_table.CorrelationTable
import ch.unil.pafanalysis.analysis.steps.one_d_enrichment.OneDEnrichment
import com.google.gson.Gson
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

@Service
class AsyncAnalysisStepService {

    @Autowired
    private var env: Environment? = null

    @Autowired
    private var analysisStepRepository: AnalysisStepRepository? = null

    val gson = Gson()

    @Async
    fun copyDuplicatedStepFiles(newSteps: List<AnalysisStep>, analysisId: Int?) {
        val outputRoot = env?.getProperty("output.path")

        newSteps.forEach { newStep ->
            val resultPath = "$analysisId/${newStep.id}"
            File(outputRoot + resultPath).mkdir()

            val oldStep = analysisStepRepository?.findById(newStep.id!!)

            val newFile: String? = if(newStep.modifiesResult == true){
                copyResultTable(oldStep, resultPath, outputRoot)
            }else{
                if(newStep.beforeId != null) analysisStepRepository?.findById(newStep.beforeId)?.resultTablePath else null
            }

            addExtraTables(oldStep, resultPath, outputRoot)
            analysisStepRepository?.saveAndFlush(newStep.copy(resultPath = resultPath, resultTablePath = newFile))
        }
    }

    private fun addExtraTables(analysisStep: AnalysisStep?, resultPath: String, outputRoot: String?) {
        val table: File? = when (analysisStep?.type) {
            AnalysisStepType.CORRELATION_TABLE.value -> {
                val res = gson.fromJson(analysisStep.results, CorrelationTable::class.java)
                File("$outputRoot${analysisStep.resultPath}/${res.correlationTable}")
            }
            AnalysisStepType.ONE_D_ENRICHMENT.value -> {
                val res = gson.fromJson(analysisStep.results, OneDEnrichment::class.java)
                File("$outputRoot${analysisStep.resultPath}/${res.enrichmentTable}")
            }
            else -> null
        }

        table?.let {
            copyFile(it, resultPath, outputRoot)
        }
    }

    private fun copyResultTable(oldStep: AnalysisStep?, resultPath: String, outputRoot: String?): String {
        val oldFile = File(outputRoot + oldStep?.resultTablePath)
        return copyFile(oldFile, resultPath, outputRoot)
    }

    private fun copyFile(oldFile: File, resultPath: String, outputRoot: String?): String {
        val newFile = resultPath + "/" + oldFile.name

        // create path if it doesnt exist
        Files.createDirectories(Paths.get(outputRoot + resultPath))

        oldFile.copyTo(File(outputRoot + newFile ))
        return newFile
    }

    @Transactional
    fun setAllStepsStatus(analysisStep: AnalysisStep?, status: AnalysisStepStatus) {
        if(analysisStep?.id !== null){
            setAnalysisStepStatus(analysisStep.id, status)
        }
        if (analysisStep?.nextId != null) {
            val nextStep = analysisStepRepository?.findById(analysisStep.nextId)
            if(nextStep != null) {
                setAllStepsStatus(nextStep, status)
            }
        }
    }

    fun setAnalysisStepStatus(id: Int, status: AnalysisStepStatus): Int? {
        val step = analysisStepRepository?.findById(id)
        if(step != null){
            analysisStepRepository?.saveAndFlush(step.copy(status = status.value))
        }
        return step?.id
    }



}