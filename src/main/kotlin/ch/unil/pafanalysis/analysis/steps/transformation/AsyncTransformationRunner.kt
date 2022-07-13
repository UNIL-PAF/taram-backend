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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import kotlin.math.ln

@Service
class AsyncTransformationRunner() : CommonStep() {

    private val readTableData = ReadTableData()
    private val writeTableData = WriteTableData()

    @Autowired
    val logTransformationRunner: LogTransformationRunner ? = null

    @Autowired
    val normalizationRunner: NormalizationRunner ? = null

    @Autowired
    val imputationRunner: ImputationRunner ? = null

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

    fun transformTable(
        step: AnalysisStep?,
        transformationParams: TransformationParams,
        outputRoot: String?
    ): Pair<Long, CommonResult?> {
        val expDetailsTable = step?.columnInfo?.columnMapping?.experimentNames?.map { name ->
            step?.columnInfo?.columnMapping?.experimentDetails?.get(name)
        }?.filter { it?.isSelected ?: false }

        val ints =
            readTableData.getListOfInts(expInfoList = expDetailsTable, analysisStep = step, outputRoot = outputRoot)
        val transInts = logTransformationRunner!!.runTransformation(ints, transformationParams)
        val normInts = normalizationRunner!!.runNormalization(transInts, transformationParams)
        val impInts = imputationRunner!!.runImputation(normInts, transformationParams)
        val intCol = transformationParams.intCol ?: step?.commonResult?.intCol

        val newColName = "trans $intCol"

        val resTable = writeTableData.writeTable(step, impInts, outputRoot = outputRoot, newColName)
        val resTableHash = Crc32HashComputations().computeFileHash(File(resTable))
        val numCols =
            step?.commonResult?.numericalColumns?.filter { it != step?.commonResult?.intCol }?.plus(newColName)
        val commonRes = step?.commonResult?.copy(intCol = newColName, numericalColumns = numCols)
        return Pair(resTableHash, commonRes)
    }

}