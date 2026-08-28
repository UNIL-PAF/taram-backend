package ch.unil.pafanalysis.analysis.steps.normalization

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.summary_stat.SummaryStat
import ch.unil.pafanalysis.common.HeaderTypeMapping
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.SummaryStatComputation
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.common.WriteTableData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class AsyncNormalizationRunner() : CommonStep() {

    private val readTableData = ReadTableData()
    private val writeTableData = WriteTableData()
    private val hMap = HeaderTypeMapping()

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
    ): Normalization? {
        val intCol = params.intCol ?: step?.columnInfo?.columnMapping?.intCol
        val table = readTableData.getTable(getOutputRoot() + step?.resultTablePath, step?.commonResult?.headers)
        val (selHeaders, ints) = readTableData.getDoubleMatrix(table, intCol, step?.columnInfo?.columnMapping?.experimentDetails)

        val normInts = normComp!!.runNormalization(step, ints, params, intCol)

        val newCols: List<List<Any>>? = table.cols?.mapIndexed { i, c ->
            val selHeader = selHeaders.withIndex().find { it.value.idx == i }
            if (selHeader != null) {
                normInts[selHeader.index]
            } else c
        }

        writeTableData.write(getOutputRoot() + step?.resultTablePath, table.copy(cols = newCols))

        val summaryStatComp = SummaryStatComputation()
        val basicStat = summaryStatComp.getBasicSummaryStat(normInts, selHeaders)

        val selGenes = if(!params.selProts.isNullOrEmpty()) getSelGenes(table, params, step?.analysis?.result?.type) else null

        return Normalization(
            min = basicStat.min?.first(),
            max = basicStat.max?.first(),
            mean = basicStat.mean?.first(),
            median = basicStat.median?.first(),
            sum = basicStat.sum?.first(),
            nrValid = basicStat.nrValid?.first(),
            nrNaN = basicStat.nrNaN?.first(),
            selGenes = selGenes
        )
    }

    private fun getSelGenes(table: Table?,
                            params: NormalizationParams?,
                            resType: String?): List<String>? {

        val protGroup =
            readTableData.getStringColumn(table, hMap.getCol("proteinIds", resType))?.map { it.split(";")[0] }
        val genes = readTableData.getStringColumn(table, hMap.getCol("geneNames", resType))?.map { it.split(";")[0] }

        return params?.selProts?.map { p ->
            val i = protGroup?.indexOf(p)
            if (i != null && i >= 0) {
                genes?.get(i)
            } else null
        }?.filterNotNull()
    }

}