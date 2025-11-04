package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.Analysis
import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.EchartsPlot
import ch.unil.pafanalysis.analysis.steps.initial_result.InitialResultRunner
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

@Service
class AsyncAnalysisStepService {

    @Autowired
    private var env: Environment? = null

    @Autowired
    private var analysisStepRepository: AnalysisStepRepository? = null

    @Async
    fun copyDuplicatedStepFiles(newSteps: List<AnalysisStep>, analysisId: Int?) {
        val outputRoot = env?.getProperty("output.path")

        newSteps.forEach { newStep ->
            val resultPath = "$analysisId/${newStep.id}"
            File(outputRoot + resultPath).mkdir()
            val newFile: String? = if(newStep.modifiesResult == true){
                copyFile(newStep, resultPath, outputRoot)
            }else{
                if(newStep.beforeId != null) analysisStepRepository?.findById(newStep.beforeId!!)?.resultTablePath else null
            }
            analysisStepRepository?.saveAndFlush(newStep.copy(resultPath = resultPath, resultTablePath = newFile))
        }
    }

    private fun copyFile(newStep: AnalysisStep, resultPath: String, outputRoot: String?): String {
        val oldStep = analysisStepRepository?.findById(newStep.id!!)
        val oldFile = File(outputRoot + oldStep?.resultTablePath)
        val newFile = resultPath + "/" + oldFile.name
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