package ch.unil.pafanalysis.analysis.steps.transformation

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.steps.CommonResult
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.Crc32HashComputations
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.WriteTableData
import ch.unil.pafanalysis.results.model.ResultType
import com.google.common.math.Quantiles
import com.google.gson.Gson
import org.springframework.stereotype.Service
import java.io.*
import kotlin.math.log2

@Service
class TransformationRunner() : CommonStep() {

    private val gson = Gson()

    override var type: AnalysisStepType? = AnalysisStepType.BOXPLOT

    private val readTableData = ReadTableData()
    private val writeTableData = WriteTableData()

    private var columnMapping: ColumnMapping? = null

    fun run(oldStepId: Int): AnalysisStepStatus {
        val newStep = runCommonStep(AnalysisStepType.TRANSFORMATION, oldStepId, true)
        columnMapping = newStep?.columnInfo?.columnMapping

        val defaultParams = TransformationParams(
            normalizationType = NormalizationType.MEDIAN.value,
            transformationType = TransformationType.LOG2.value,
            imputationType = ImputationType.NAN.value
        )

        val resultTableHash = transformTable(newStep, defaultParams)
        val stepWithRes = newStep?.copy(parameters = gson.toJson(defaultParams), resultTableHash = resultTableHash)
        val oldStep = analysisStepRepository?.findById(oldStepId)
        val newHash = computeStepHash(stepWithRes, oldStep)

        val updatedStep = stepWithRes?.copy(status = AnalysisStepStatus.DONE.value, stepHash = newHash)
        println(analysisStepRepository)
        analysisStepRepository?.save(updatedStep!!)
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

    private fun transformTable(step: AnalysisStep?, transformationParams: TransformationParams): Long {
        val expDetailsTable = columnMapping?.experimentNames?.map { name ->
            columnMapping?.experimentDetails?.get(name)
        }?.filter { it?.isSelected ?: false }

        val ints = readTableData.getListOfInts(expInfoList = expDetailsTable, analysisStep = step, outputRoot = outputRoot)
        val normInts = normalization(ints, transformationParams)

        val resTable = writeTableData.writeTable(step, normInts, outputRoot = outputRoot, "trans")
        return Crc32HashComputations().computeFileHash(File(resTable))
    }



    private fun normalization(ints: List<Pair<String, List<Double>>>, transformationParams: TransformationParams): List<Pair<String, List<Double>>> {
        val subtract = if(transformationParams.normalizationType == NormalizationType.MEDIAN.value){
            fun (orig: List<Double>): Double { return Quantiles.median().compute(orig) }
        }else{
            throw StepException("${transformationParams.normalizationType} is not implemented.")
        }

        return ints.map{(name, orig: List<Double>) ->
            Pair(name, orig.map{ it - subtract(orig) })
        }
    }

}