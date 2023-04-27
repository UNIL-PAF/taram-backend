package ch.unil.pafanalysis.analysis.steps.summary_stat

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.SummaryStatComputation
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class AsyncSummaryStatRunner() : CommonStep() {

    private val readTableData = ReadTableData()

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val params = gson.fromJson(newStep?.parameters, SummaryStatParams().javaClass)

            val normRes = transformTable(
                newStep,
                params
            )

            newStep?.copy(
                results = gson.toJson(normRes)
            )
        }

        tryToRun(funToRun, newStep)
    }

    fun transformTable(
        step: AnalysisStep?,
        params: SummaryStatParams,
    ): SummaryStat? {
        val intCol = params.intCol ?: step?.columnInfo?.columnMapping?.intCol
        val table = readTableData.getTable(getOutputRoot() + step?.resultTablePath, step?.commonResult?.headers)
        val (headers, ints) = readTableData.getDoubleMatrix(table, intCol)

        val summaryStatComp = SummaryStatComputation()
        return summaryStatComp.getSummaryStat(ints, headers)
    }

}