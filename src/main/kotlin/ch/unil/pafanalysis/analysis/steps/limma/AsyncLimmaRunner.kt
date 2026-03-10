package ch.unil.pafanalysis.analysis.steps.limma

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.WriteTableData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class AsyncLimmaRunner() : CommonStep() {

    private val readTableData = ReadTableData()
    private val writeTableData = WriteTableData()

    @Autowired
    val limmaComputation: LimmaComputation? = null

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val res = computeTTest(newStep)

            newStep?.copy(
                results = gson.toJson(res.limma),
                commonResult = newStep.commonResult?.copy(headers = res.headers)
            )
        }
        tryToRun(funToRun, newStep)
    }

    data class LimmaRes(val limma: Limma?, val headers: List<Header>?)

    fun computeTTest(
        step: AnalysisStep?
    ): LimmaRes {
        val outputRoot = getOutputRoot()
        val params = gson.fromJson(step?.parameters, LimmaParams().javaClass)
        val table = readTableData.getTable(outputRoot + step?.resultTablePath, step?.commonResult?.headers)
        val (resTable, tTestRes) = limmaComputation?.run(table, params, step)!!
        writeTableData.write(outputRoot + step?.resultTablePath!!, resTable!!)
        return LimmaRes(tTestRes, resTable.headers)
    }
}