package ch.unil.pafanalysis.analysis.steps.t_test

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.Crc32HashComputations
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.WriteTableData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.File

@Service
class AsyncTTestRunner() : CommonStep() {

    private val readTableData = ReadTableData()
    private val writeTableData = WriteTableData()

    @Autowired
    val tTestComputation: TTestComputation? = null

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?, paramsString: String?) {
        try {
            val defaultResult = TTest()

            val (tTestRes, resultTableHash) = computeTTest(
                newStep,
                gson.fromJson(paramsString, TTestParams().javaClass),
                getOutputRoot()
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
                    results = gson.toJson(tTestRes)
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

    fun computeTTest(
        step: AnalysisStep?,
        params: TTestParams,
        outputRoot: String?
    ): Triple<TTest, Long, String> {
        val table = readTableData.getTable(outputRoot + step?.resultTablePath, step?.columnInfo?.columnMapping)
        val (resTable, nrSign) = tTestComputation?.run(table, params, step?.columnInfo)!!
        writeTableData?.write(outputRoot + step?.resultTablePath!!, resTable!!)
        val resFileHash = Crc32HashComputations().computeFileHash(File(outputRoot + step?.resultTablePath))
        val resFilePath = step?.resultTablePath!!
        return Triple(TTest(nrSign), resFileHash, resFilePath)
    }
}