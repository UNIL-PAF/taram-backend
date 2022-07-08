package ch.unil.pafanalysis.analysis.steps.transformation

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.steps.CommonResult
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.Crc32HashComputations
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.WriteTableData
import com.google.common.math.Quantiles
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.File
import kotlin.math.ln

@Service
class AsyncTransformationRunner() : CommonStep() {

    private val readTableData = ReadTableData()
    private val writeTableData = WriteTableData()

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?, paramsString: String?) {
        try {
            val defaultResult = Transformation(newStep?.commonResult?.numericalColumns, newStep?.commonResult?.intCol)

            val (resultTableHash, commonResult) = transformTable(
                newStep,
                gson.fromJson(paramsString, TransformationParams().javaClass),
                getOutputRoot(getResultType(newStep?.analysis?.result?.type))
            )
            val stepWithRes = newStep?.copy(
                parameters = paramsString,
                parametersHash = hashComp.computeStringHash(paramsString),
                resultTableHash = resultTableHash,
                results = gson.toJson(defaultResult)
            )
            val oldStep = analysisStepRepository?.findById(oldStepId)
            val newHash = computeStepHash(stepWithRes, oldStep)

            val updatedStep =
                stepWithRes?.copy(
                    status = AnalysisStepStatus.DONE.value,
                    stepHash = newHash,
                    commonResult = commonResult
                )
            analysisStepRepository?.saveAndFlush(updatedStep!!)!!
            updateNextStep(updatedStep!!)
        } catch (e: Exception) {
            println("Error in transformation asyncRun ${newStep?.id}")
            e.printStackTrace()
            analysisStepRepository?.saveAndFlush(
                newStep!!.copy(
                    status = AnalysisStepStatus.ERROR.value,
                    error = e.message,
                    stepHash = Crc32HashComputations().getRandomHash()
                )
            )
        }
    }

    private fun transformTable(
        step: AnalysisStep?,
        transformationParams: TransformationParams,
        outputRoot: String?
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
            val a: List<Double> = when (transformationParams.transformationType) {
                TransformationType.NONE.value -> intList
                TransformationType.LOG2.value -> {
                    val newList = intList.map { i ->
                        if (i == 0.0) {
                            Double.NaN
                        } else {
                            ln(i)// / ln(2.0)
                        }
                    }
                    newList
                }
                else -> {
                    throw StepException("${transformationParams.normalizationType} is not implemented.")
                }
            }
            return a
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
            val noNaNs = orig.filter { !it.isNaN() }
            Pair(name, orig.map { it - subtract(noNaNs) })
        }
    }

}