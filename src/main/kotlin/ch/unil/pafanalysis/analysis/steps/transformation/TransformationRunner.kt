package ch.unil.pafanalysis.analysis.steps.transformation

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.results.model.ResultType
import com.google.common.math.Quantiles
import com.google.gson.Gson
import org.springframework.stereotype.Service
import kotlin.math.log2

@Service
class TransformationRunner() : CommonStep() {

    override var type: AnalysisStepType? = AnalysisStepType.BOXPLOT

    private val gson = Gson()

    private var columnMapping: ColumnMapping? = null

    fun run(oldStepId: Int): AnalysisStepStatus {
        val newStep = runCommonStep(AnalysisStepType.TRANSFORMATION, oldStepId, true)
        columnMapping = newStep?.columnInfo?.columnMapping
        //val updatedStep = newStep?.copy(status = AnalysisStepStatus.DONE.value, results = gson.toJson(boxplot))
        //analysisStepRepository?.save(updatedStep!!)
        return AnalysisStepStatus.DONE
    }

    fun updateParams(analysisStep: AnalysisStep, params: String): AnalysisStepStatus {
        setPathes(analysisStep.analysis)
        columnMapping = analysisStep?.columnInfo?.columnMapping
        val stepWithParams = analysisStep.copy(parameters = params)

        val oldStep = if(analysisStep?.beforeId != null) analysisStepRepository?.findById(analysisStep?.beforeId) else null
        val newHash = computeStepHash(stepWithParams, oldStep)

        //val newStep = stepWithParams.copy(status = AnalysisStepStatus.DONE.value, results = gson.toJson(boxplot), stepHash = newHash)
        //analysisStepRepository?.save(newStep!!)
        return AnalysisStepStatus.DONE
    }

    override fun computeAndUpdate(step: AnalysisStep, stepBefore: AnalysisStep, newHash: Long) {
        setPathes(step.analysis)
        columnMapping = step?.columnInfo?.columnMapping
        val stepWithNewResTable = step.copy(resultTableHash = stepBefore?.resultTableHash, resultTablePath = stepBefore?.resultTablePath)

        //val stepToSave = stepWithNewResTable.copy(results = gson.toJson(boxplot))
        //analysisStepRepository?.save(stepToSave)
    }

}