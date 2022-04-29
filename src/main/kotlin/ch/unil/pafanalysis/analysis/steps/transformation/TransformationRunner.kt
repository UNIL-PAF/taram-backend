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

        val defaultResult = Transformation(newStep?.commonResult?.numericalColumns, newStep?.commonResult?.intCol)

        val (resultTableHash, commonResult) = transformTable(newStep, defaultParams)
        val stepWithRes = newStep?.copy(parameters = gson.toJson(defaultParams), resultTableHash = resultTableHash, results = gson.toJson(defaultResult))
        val oldStep = analysisStepRepository?.findById(oldStepId)
        val newHash = computeStepHash(stepWithRes, oldStep)

        val updatedStep = stepWithRes?.copy(status = AnalysisStepStatus.DONE.value, stepHash = newHash, commonResult = commonResult)
        analysisStepRepository?.save(updatedStep!!)
        return AnalysisStepStatus.DONE
    }

    fun updateParams(analysisStep: AnalysisStep, params: String): AnalysisStepStatus {
        setPathes(analysisStep.analysis)
        columnMapping = analysisStep?.columnInfo?.columnMapping

        val parsedParams: TransformationParams = gson.fromJson(params, TransformationParams().javaClass)
        val (resultTableHash, commonResult) = transformTable(analysisStep, parsedParams)
        val stepWithRes = analysisStep?.copy(parameters = params, resultTableHash = resultTableHash, commonResult = commonResult)
        val oldStep = analysisStepRepository?.findById(stepWithRes?.beforeId!!)
        val newHash = computeStepHash(stepWithRes, oldStep)

        val newStep = stepWithRes.copy(status = AnalysisStepStatus.DONE.value, stepHash = newHash)
        analysisStepRepository?.save(newStep!!)
        return AnalysisStepStatus.DONE
    }

    override fun computeAndUpdate(step: AnalysisStep, stepBefore: AnalysisStep, newHash: Long) {
        setPathes(step.analysis)
        columnMapping = step?.columnInfo?.columnMapping
        val stepWithNewResTable = step.copy(resultTableHash = stepBefore?.resultTableHash, resultTablePath = stepBefore?.resultTablePath)

        //val stepToSave = stepWithNewResTable.copy(results = gson.toJson(boxplot))
        //analysisStepRepository?.save(stepToSave)
    }

    private fun transformTable(step: AnalysisStep?, transformationParams: TransformationParams): Pair<Long, CommonResult?> {
        val expDetailsTable = columnMapping?.experimentNames?.map { name ->
            columnMapping?.experimentDetails?.get(name)
        }?.filter { it?.isSelected ?: false }

        val ints = readTableData.getListOfInts(expInfoList = expDetailsTable, analysisStep = step, outputRoot = outputRoot)
        val normInts = normalization(ints, transformationParams)

        val intCol = if(transformationParams != null && transformationParams.intCol != null) transformationParams.intCol else step?.commonResult?.intCol

        val newColName = "trans $intCol"
        val resTable = writeTableData.writeTable(step, normInts, outputRoot = outputRoot, newColName)
        val resTableHash =  Crc32HashComputations().computeFileHash(File(resTable))
        val commonRes = step?.commonResult?.copy(intCol = newColName, numericalColumns = step?.commonResult?.numericalColumns?.plus(newColName))
        return Pair(resTableHash, commonRes)
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