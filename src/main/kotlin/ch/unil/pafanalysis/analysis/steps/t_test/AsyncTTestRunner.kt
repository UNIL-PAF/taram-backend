package ch.unil.pafanalysis.analysis.steps.t_test

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.model.ColumnMapping
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.group_filter.GroupFilterParams
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
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val res = computeTTest(newStep)

            newStep?.copy(
                results = gson.toJson(res.tTest),
                commonResult = newStep?.commonResult?.copy(headers = res.headers)
            )
        }
        tryToRun(funToRun, newStep)
    }

    data class TTestRes(val tTest: TTest?, val headers: List<Header>?)

    fun computeTTest(
        step: AnalysisStep?
    ): TTestRes {
        val outputRoot = getOutputRoot()
        val params = gson.fromJson(step?.parameters, TTestParams().javaClass)
        val table = readTableData.getTable(outputRoot + step?.resultTablePath, step?.commonResult?.headers)
        val (resTable, nrSign, headers) = tTestComputation?.run(table, params, step?.columnInfo)!!
        writeTableData?.write(outputRoot + step?.resultTablePath!!, resTable!!)
        return TTestRes(TTest(nrSign), headers)
    }
}