package ch.unil.pafanalysis.analysis.steps.summary_stat

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.ExpInfo
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.SummaryStatComputation
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.results.model.ResultType
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

    private fun orderHeaders(headers: List<Header>, orderByGroups: Boolean?, groupsOrdered: List<String>?, expDetails: Map<String, ExpInfo>?): List<Header> {
        return if(orderByGroups == true && groupsOrdered?.isNotEmpty() == true){
            groupsOrdered.fold(emptyList<Header>()){acc, v ->
                val groupH = headers.filter{h -> expDetails?.get(h.experiment?.name)?.group == v}
                acc.plus(groupH)
            }
        } else headers
    }

    private fun orderInts(origIdx: List<Int>, orderedIdx: List<Int>, ints: List<List<Double>>): List<List<Double>> {
        return orderedIdx.fold(emptyList()){ acc, v ->
            val pos = origIdx.indexOf(v)
            acc.plusElement(ints[pos])
        }
    }

    private fun orderPeps(origIdx: List<Int>, orderedIdx: List<Int>, peps: List<Int>?): List<Int>? {
        if(peps == null) return null
        return orderedIdx.fold(emptyList()){ acc, v ->
            val pos = origIdx.indexOf(v)
            acc.plusElement(peps[pos])
        }
    }

    private fun getNrPeps(origIdx: List<Int>, orderedIdx: List<Int>, step: AnalysisStep?, table: Table?): Pair<List<Int>?, String?> {
        val headers = step?.commonResult?.headers

        val (selHeaders, pepField) = when (step?.analysis?.result?.type) {
            ResultType.MaxQuant.value ->
                headers?.filter { it.experiment?.field == "Razor.unique.peptides" } to
                        "Razor.unique.peptides"

            ResultType.Spectronaut.value -> {
                val field = if (headers?.any { it.experiment?.field == "NrOfStrippedSequencesIdentified" } == true)
                    "NrOfStrippedSequencesIdentified"
                else
                    "NrOfPrecursorsIdentified"
                headers?.filter { it.experiment?.field == field } to field
            }
            else -> null to null
        }

        return if(!selHeaders.isNullOrEmpty()){
            val nrPeps = readTableData.getDoubleMatrix(table, selHeaders).map{a -> a.sumOf { it.toInt() } }
            orderPeps(origIdx, orderedIdx, nrPeps) to pepField
        } else null to null
    }

    fun transformTable(
        step: AnalysisStep?,
        params: SummaryStatParams,
    ): SummaryStat? {
        val expDetails = step?.columnInfo?.columnMapping?.experimentDetails
        val intCol = params.intCol ?: step?.columnInfo?.columnMapping?.intCol
        val table = readTableData.getTable(getOutputRoot() + step?.resultTablePath, step?.commonResult?.headers)
        val (headers, ints) = readTableData.getDoubleMatrix(table, intCol, expDetails)
        val groupsOrdered = step?.columnInfo?.columnMapping?.groupsOrdered
        val orderedHeaders = orderHeaders(headers, params.orderByGroups, groupsOrdered, expDetails)
        val origIdx = headers.map{it.idx}
        val orderedIdx = orderedHeaders.map{it.idx}
        val orderedInts = orderInts(origIdx, orderedIdx, ints)
        val summaryStat = SummaryStatComputation().getSummaryStat(orderedInts, orderedHeaders, expDetails)
        val (nrPeps, pepField) = getNrPeps(origIdx, orderedIdx, step, table)
        return summaryStat.copy(nrOfPeps = nrPeps, pepField = pepField)
    }

}