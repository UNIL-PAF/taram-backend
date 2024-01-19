package ch.unil.pafanalysis.analysis.steps.log_transformation

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.summary_stat.SummaryStat
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.SummaryStatComputation
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.common.WriteTableData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class AsyncLogTransformationRunner() : CommonStep() {

    private val readTableData = ReadTableData()
    private val writeTableData = WriteTableData()

    @Autowired
    val logComp: LogTransformationComputation? = null

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val params = gson.fromJson(newStep?.parameters, LogTransformationParams().javaClass)
            val table = readTableData.getTable(getOutputRoot() + newStep?.resultTablePath, newStep?.commonResult?.headers)
            val (selHeaders, ints) = getSelTable(table, newStep, params)
            val transInts = logComp!!.runTransformation(ints, params)
            val newCols = computeTransTable(transInts, table, selHeaders, params)
            val newHeaders = adaptColNames(selHeaders, table.headers, params)
            writeTableData.write(getOutputRoot() + newStep?.resultTablePath, Table(newHeaders, newCols))
            val logTransRes = getTransformation(selHeaders, transInts)

            val commonResult =
                newStep?.commonResult?.copy(
                    intColIsLog = params.transformationType == TransformationType.LOG2.value,
                    headers = newHeaders
                )

            newStep?.copy(
                results = gson.toJson(logTransRes),
                commonResult = commonResult,
                )
        }
        tryToRun(funToRun, newStep)
    }

    private fun getSelTable(table: Table?, step: AnalysisStep?, params: LogTransformationParams): Pair<List<Header>, List<List<Double>>> {
        val (selHeaders, ints) = if(params.selColIdxs != null){
            val selCols = table?.headers?.filter{params.selColIdxs.contains(it.idx)} ?: emptyList()
            Pair(selCols, readTableData.getDoubleMatrix(table, selCols))
        }else{
            val intCol = params.intCol ?: step?.columnInfo?.columnMapping?.intCol
            readTableData.getDoubleMatrix(table, intCol, step?.columnInfo?.columnMapping?.experimentDetails)
        }
        return Pair(selHeaders, ints)
    }

    private fun adaptColNames(selHeaders: List<Header>, tableHeaders: List<Header>?, params: LogTransformationParams): List<Header>? {
        return if(params.adaptHeaders == true){
            val selHeaderIdxs = selHeaders.map{it.idx}
            tableHeaders?.map{ h ->
                if(selHeaderIdxs.contains(h.idx)){
                    h.copy(name = h.name + "." + params.transformationType)
                }else h
            }
        } else tableHeaders
    }

    private fun computeTransTable(transInts: List<List<Double>>, table: Table?, selHeaders: List<Header>, params: LogTransformationParams): List<List<Any>>?{
        return table?.cols?.mapIndexed { i, c ->
            val selHeader = selHeaders.withIndex().find { it.value.idx == i }
            if (selHeader != null) {
                transInts[selHeader.index]
            } else c
        }
    }

    private fun getTransformation(selHeaders: List<Header>, transInts: List<List<Double>>): LogTransformation? {
        val summaryStatComp = SummaryStatComputation()
        val stat = summaryStatComp.getBasicSummaryStat(transInts, selHeaders)

        return LogTransformation(
            min = stat.min?.first(),
            max = stat.max?.first(),
            mean = stat.mean?.first(),
            median = stat.median?.first(),
            nrNaN = stat.nrNaN?.first(),
            nrValid = stat.nrValid?.first(),
            sum = stat.sum?.first()
        )
    }

}