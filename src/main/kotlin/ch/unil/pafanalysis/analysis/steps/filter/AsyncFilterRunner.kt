package ch.unil.pafanalysis.analysis.steps.filter

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.steps.CommonResult
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.analysis.steps.transformation.LogTransformationRunner
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
class AsyncFilterRunner() : CommonStep() {

    private val readTableData = ReadTableData()
    private val writeTableData = WriteTableData()

    @Autowired
    val fixFilterRunner: FixFilterRunner? = null

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?, paramsString: String?) {
        try {
            val defaultResult = Filter()

            val (filterRes, resultTableHash) = filterTable(
                newStep,
                gson.fromJson(paramsString, FilterParams().javaClass),
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
                    results = gson.toJson(filterRes)
                )
            analysisStepRepository?.saveAndFlush(updatedStep!!)!!
            updateNextStep(updatedStep!!)
        } catch (e: Exception) {
            println("Error in filter asyncRun ${newStep?.id}")
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

    fun filterTable(
        step: AnalysisStep?,
        params: FilterParams,
        outputRoot: String?
    ): Triple<Filter, Long, String> {
        val table = readTableData.getTable(outputRoot + step?.resultTablePath, null)
        val fltTable = fixFilterRunner?.run(table, params, step?.columnInfo)
        val fltSize = fltTable?.cols?.size
        val nrRowsRemoved = table.cols?.size?.minus(fltSize!!)
        val resFileHash = (234234).toLong()
        val resFilePath = "blibla"
        return Triple(Filter(fltSize, nrRowsRemoved), resFileHash, resFilePath)
    }

}