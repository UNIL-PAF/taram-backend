package ch.unil.pafanalysis.analysis.steps

import ch.unil.pafanalysis.analysis.model.Analysis
import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import ch.unil.pafanalysis.analysis.service.AnalysisStepRepository
import ch.unil.pafanalysis.results.model.ResultType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.io.File
import java.sql.Timestamp
import java.time.LocalDateTime

@Service
open class CommonStep {

    @Autowired
    var analysisStepRepository: AnalysisStepRepository? = null

    @Autowired
    var analysisRepository: AnalysisRepository? = null

    @Autowired
    private var env: Environment? = null

    var type: AnalysisStepType? = null

    var outputRoot: String? = null
    var resultPath: String? = null
    var resultType: ResultType? = null

    fun runCommonStep(type: AnalysisStepType, oldStepId: Int? = null, modifiesResult: Boolean? = null): AnalysisStep?{
        val oldStep: AnalysisStep? = if(oldStepId != null){analysisStepRepository?.findById(oldStepId)}else{null}

        val emptyStep = createEmptyAnalysisStep(oldStep?.analysis, type, oldStep?.id, oldStep?.nextId)
        val stepPath = setMainPaths(oldStep?.analysis, emptyStep)
        val resultTablePath = getResultTablePath(modifiesResult, oldStep, stepPath)

        updateOldStep(oldStep, emptyStep?.id)

        return updateEmptyStep(emptyStep, stepPath, resultTablePath, oldStep?.resultTableHash)
    }

    fun setMainPaths(analysis: Analysis?, emptyStep: AnalysisStep?): String {
        resultType =
            if (analysis?.result?.type == ResultType.MaxQuant.value) ResultType.MaxQuant else ResultType.Spectronaut
        outputRoot =
            env?.getProperty(if (resultType == ResultType.MaxQuant) "output.path.maxquant" else "output.path.spectronaut")
        resultPath =
            env?.getProperty(if (resultType == ResultType.MaxQuant) "result.path.maxquant" else "result.path.spectronaut") + analysis?.result?.path

        val outputPath: String = analysis?.id.toString()
        createResultDir(outputRoot?.plus(outputPath))

        val stepPath = "$outputPath/${emptyStep?.id}"
        createResultDir(outputRoot?.plus(stepPath))
        return stepPath
    }

    fun createEmptyAnalysisStep(analysis: Analysis?, type: AnalysisStepType, beforeId: Int? = null, nextId: Int? = null): AnalysisStep? {
        val newStep = AnalysisStep(
            status = AnalysisStepStatus.IDLE.value,
            type = type.value,
            analysis = analysis,
            lastModifDate = LocalDateTime.now(),
            nextId = nextId,
        )
        return analysisStepRepository?.save(newStep)
    }

    private fun updateEmptyStep(emptyStep: AnalysisStep? , stepPath: String?, resultTablePath: String?, resultTableHash: Long?): AnalysisStep? {
        val newStep = emptyStep?.copy(resultPath = stepPath, resultTablePath = resultTablePath, resultTableHash = resultTableHash)
        return analysisStepRepository?.save(newStep!!)
    }

    private fun updateOldStep(oldStep: AnalysisStep?, newNextId: Int?){
        val updatedOldStep = oldStep?.copy(nextId = newNextId)
        analysisStepRepository?.save(updatedOldStep!!)
    }

    private fun getResultTablePath(modifiesResult: Boolean?, oldStep: AnalysisStep?, stepPath: String?): String? {
        return if(modifiesResult != null && modifiesResult){
            val oldTab = outputRoot?.plus("/") + oldStep?.resultTablePath
            val tabName = if(resultType == ResultType.MaxQuant){"/proteinGroups_"}else{"/Report_"}
            val newTab = stepPath + tabName + Timestamp(System.currentTimeMillis()).time + ".txt"
            File(oldTab).copyTo(File( outputRoot?.plus(newTab)))
            newTab
        }else{
            oldStep?.resultTablePath
        }
    }

    private fun createResultDir(outputPath: String?): String {
        if (outputPath == null) throw RuntimeException("There is no output path defined.")
        val dir = File(outputPath)
        if (!dir.exists()) dir.mkdir()
        return outputPath
    }
}