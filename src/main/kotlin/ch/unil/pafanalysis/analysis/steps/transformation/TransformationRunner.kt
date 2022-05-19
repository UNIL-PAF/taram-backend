package ch.unil.pafanalysis.analysis.steps.transformation

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.steps.CommonResult
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.Crc32HashComputations
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.WriteTableData
import com.google.common.math.Quantiles
import com.google.gson.Gson
import org.springframework.stereotype.Service
import java.io.File
import kotlin.math.ln

@Service
class TransformationRunner() : CommonStep() {

    private val gson = Gson()

    override var type: AnalysisStepType? = AnalysisStepType.BOXPLOT

    private val readTableData = ReadTableData()
    private val writeTableData = WriteTableData()

    val defaultParams = TransformationParams(
        normalizationType = NormalizationType.MEDIAN.value,
        transformationType = TransformationType.NONE.value,
        imputationType = ImputationType.NAN.value
    )

    override fun run(oldStepId: Int, step: AnalysisStep?): AnalysisStepStatus {
        val newStep = runCommonStep(AnalysisStepType.TRANSFORMATION, oldStepId, true, step)

        val defaultResult = Transformation(newStep?.commonResult?.numericalColumns, newStep?.commonResult?.intCol)
        val params: TransformationParams = if (newStep!!.parameters != null) {
            gson.fromJson(step!!.parameters, TransformationParams().javaClass)
        } else defaultParams

        val (resultTableHash, commonResult) = transformTable(newStep, params)
        val stepWithRes = newStep?.copy(
            parameters = gson.toJson(params),
            resultTableHash = resultTableHash,
            results = gson.toJson(defaultResult)
        )
        val oldStep = analysisStepRepository?.findById(oldStepId)
        val newHash = computeStepHash(stepWithRes, oldStep)

        val updatedStep =
            stepWithRes?.copy(status = AnalysisStepStatus.DONE.value, stepHash = newHash, commonResult = commonResult)
        analysisStepRepository?.save(updatedStep!!)
        return AnalysisStepStatus.DONE
    }

    fun updateParams(analysisStep: AnalysisStep, params: String): AnalysisStepStatus {
        setPathes(analysisStep.analysis)
        analysisStepRepository?.save(analysisStep.copy(status = AnalysisStepStatus.RUNNING.value))

        val oldStep = analysisStepRepository?.findById(analysisStep.beforeId!!)
        val stepPath = setMainPaths(oldStep?.analysis, analysisStep)
        val resultTablePath = getResultTablePath(true, oldStep, stepPath).first
        val stepWithTable = analysisStep.copy(resultTablePath = resultTablePath)

        val parsedParams: TransformationParams = gson.fromJson(params, TransformationParams().javaClass)
        val (resultTableHash, commonResult) = transformTable(stepWithTable, parsedParams)
        val stepWithRes =
            stepWithTable?.copy(parameters = params, resultTableHash = resultTableHash, commonResult = commonResult)
        val newHash = computeStepHash(stepWithRes, oldStep)

        val newStep = stepWithRes.copy(status = AnalysisStepStatus.DONE.value, stepHash = newHash)
        analysisStepRepository?.save(newStep!!)

        updateNextStep(newStep)
        return AnalysisStepStatus.DONE
    }

    /*
    override fun computeAndUpdate(step: AnalysisStep, stepBefore: AnalysisStep, newHash: Long, stepPath: String) {
        val resultTablePath = getResultTablePath(true, stepBefore, stepPath).first

        val stepWithTable = step.copy(status = AnalysisStepStatus.RUNNING.value, resultTablePath = resultTablePath, commonResult = stepBefore.commonResult)
        analysisStepRepository?.save(stepWithTable)

        columnMapping = stepWithTable?.columnInfo?.columnMapping
        val (resultTableHash, commonResult) = transformTable(
            stepWithTable,
            gson.fromJson(stepWithTable.parameters, TransformationParams().javaClass)
        )

        val stepWithRes = stepWithTable.copy(resultTableHash = resultTableHash, commonResult = commonResult)
        val newHash = computeStepHash(stepWithRes, stepBefore)
        val newStep = stepWithRes.copy(status = AnalysisStepStatus.DONE.value, stepHash = newHash)
        analysisStepRepository?.save(newStep!!)
    }
     */

    private fun transformTable(
        step: AnalysisStep?,
        transformationParams: TransformationParams
    ): Pair<Long, CommonResult?> {
        val expDetailsTable = step?.columnInfo?.columnMapping?.experimentNames?.map { name ->
            step?.columnInfo?.columnMapping?.experimentDetails?.get(name)
        }?.filter { it?.isSelected ?: false }

        val ints =
            readTableData.getListOfInts(expInfoList = expDetailsTable, analysisStep = step, outputRoot = outputRoot)
        val transInts = transformation(ints, transformationParams)
        val normInts: List<Pair<String, List<Double>>> = normalization(transInts, transformationParams)
        val intCol = transformationParams.intCol ?: step?.commonResult?.intCol

        val newColName = "trans $intCol"

        val resTable = writeTableData.writeTable(step, normInts, outputRoot = outputRoot, newColName)
        val resTableHash = Crc32HashComputations().computeFileHash(File(resTable))
        val numCols =
            step?.commonResult?.numericalColumns?.filter { it != step?.commonResult?.intCol }?.plus(newColName)
        val commonRes = step?.commonResult?.copy(intCol = newColName, numericalColumns = numCols)
        return Pair(resTableHash, commonRes)
    }

    private fun transformation(
        ints: List<Pair<String, List<Double>>>,
        transformationParams: TransformationParams
    ): List<Pair<String, List<Double>>> {

        fun transformList(intList: List<Double>): List<Double> {
            return when (transformationParams.transformationType) {
                TransformationType.NONE.value -> intList
                TransformationType.LOG2.value -> intList.map { i -> if (i == 0.0) Double.NaN else ln(i) / ln(2.0) }
                else -> {
                    throw StepException("${transformationParams.normalizationType} is not implemented.")
                }
            }
        }

        return ints.map { (name, orig: List<Double>) ->
            Pair(name, transformList(orig))
        }
    }


    private fun normalization(
        ints: List<Pair<String, List<Double>>>,
        transformationParams: TransformationParams
    ): List<Pair<String, List<Double>>> {
        val subtract = when (transformationParams.normalizationType) {
            NormalizationType.MEDIAN.value -> fun(orig: List<Double>): Double {
                return Quantiles.median().compute(orig)
            }
            NormalizationType.MEAN.value -> fun(orig: List<Double>): Double { return orig.average() }
            else -> {
                throw StepException("${transformationParams.normalizationType} is not implemented.")
            }
        }
        return ints.map { (name, orig: List<Double>) ->
            Pair(name, orig.map { it - subtract(orig) })
        }
    }

}