package ch.unil.pafanalysis.analysis.steps.normalization

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.summary_stat.SummaryStat
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.SummaryStatComputation
import ch.unil.pafanalysis.common.WriteTableData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class AsyncNormalizationRunner() : CommonStep() {

    private val readTableData = ReadTableData()
    private val writeTableData = WriteTableData()

    @Autowired
    val normComp: NormalizationComputation? = null

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val params = gson.fromJson(newStep?.parameters, NormalizationParams().javaClass)

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
        params: NormalizationParams,
    ): SummaryStat? {
        val intCol = params.intCol ?: step?.columnInfo?.columnMapping?.intCol
        val table = readTableData.getTable(getOutputRoot() + step?.resultTablePath, step?.commonResult?.headers)
        val (selHeaders, ints) = readTableData.getDoubleMatrix(table, intCol)

        val normInts = normComp!!.runNormalization(ints, params)

        val newCols: List<List<Any>>? = table.cols?.mapIndexed { i, c ->
            val selHeader = selHeaders.withIndex().find { it.value.idx == i }
            if (selHeader != null) {
                normInts[selHeader.index]
            } else c
        }

        writeTableData.write(getOutputRoot() + step?.resultTablePath, table.copy(cols = newCols))

        val summaryStatComp = SummaryStatComputation()
        return summaryStatComp.getBasicSummaryStat(normInts, selHeaders)
    }

}