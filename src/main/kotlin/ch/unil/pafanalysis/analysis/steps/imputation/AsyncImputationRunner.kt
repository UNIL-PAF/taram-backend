package ch.unil.pafanalysis.analysis.steps.imputation

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class AsyncImputationRunner() : CommonStep() {

    private val readTableData = ReadTableData()
    private val writeTableData = WriteTableData()
    private val writeImputation = WriteImputationTableData()

    @Autowired
    val imputationRunner: ImputationComputation? = null

    @Autowired
    private var env: Environment? = null

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val params = gson.fromJson(newStep?.parameters, ImputationParams().javaClass)

            val (nrImputed, nrImputedGroups, imputationFileName) = transformTable(
                newStep,
                params
            )

            newStep?.copy(
                results = gson.toJson(Imputation(nrImputed, nrImputedGroups)),
                imputationTablePath = imputationFileName
            )
        }

        tryToRun(funToRun, newStep)
    }

    fun transformTable(
        step: AnalysisStep?,
        params: ImputationParams
    ): Triple<Int?, Int?, String?> {
        val table = readTableData.getTable(getOutputRoot() + step?.resultTablePath, step?.commonResult?.headers)

        val (selHeaders, ints) = if(params.intCol == null && params.selColIdxs.isNullOrEmpty()) {
            readTableData.getDoubleMatrix(table, step?.columnInfo?.columnMapping?.intCol, step?.columnInfo?.columnMapping?.experimentDetails)
        } else if(params.intCol != null) {
            val selHeaders = table.headers?.filter{h -> h.experiment?.field == params.intCol}
            Pair(selHeaders, readTableData.getDoubleMatrix(table, selHeaders ?: emptyList()))
        }else{
            val selHeaders = table.headers?.filter{h -> params.selColIdxs?.contains(h.idx) ?: false}
            Pair(selHeaders, readTableData.getDoubleMatrix(table, selHeaders ?: emptyList()))
        }
        val (impInts, imputedRows) = imputationRunner!!.runImputation(ints, params)

        val newCols: List<List<Any>>? = table.cols?.mapIndexed { i, c ->
            val selHeader = selHeaders?.withIndex()?.find { it.value.idx == i }
            if (selHeader != null) {
                impInts[selHeader.index]
            } else c
        }

        writeTableData.write(getOutputRoot() + step?.resultTablePath, table.copy(cols = newCols))

        return if (imputedRows != null) {
            val imputationTable = ImputationTable(selHeaders, imputedRows)
            val mergedImpTable = mergeImputationTables(imputationTable, step?.imputationTablePath, table.headers)
            val imputationFileName = step?.resultTablePath?.replace(".txt", "_imputation.txt")
            writeImputation.write(getOutputRoot() + imputationFileName, mergedImpTable)
            val nrImputed: Int = imputedRows.sumOf { r -> r.sumOf { (if (it) 1 else 0).toInt() } }
            val nrImputedGroups: Int? = imputedRows.map { r -> if(r.find { it } == true) 1 else 0 }.sum()
            Triple(nrImputed, nrImputedGroups, imputationFileName)
        } else Triple(null, null, null)

    }

    private fun mergeImputationTables(newImpTable: ImputationTable, oldImpTablePath: String?, headers: List<Header>?): ImputationTable {
        return if(oldImpTablePath != null){
            val oldImputationTable = ReadImputationTableData().getTable(
                env?.getProperty("output.path").plus(oldImpTablePath),
                headers
            )

            val oldImpHeaderIdxs = oldImputationTable.headers?.map{it.idx}
            val keepNew = newImpTable.headers?.map{ h -> !(oldImpHeaderIdxs?.contains(h.idx) ?: false) }
            // we ignore columns from the new table if they are in the old table
            val keptNewHeaders: List<Header>? = newImpTable.headers?.filterIndexed{i, _ -> keepNew?.get(i) ?: false}
            val keptNewMatrix = newImpTable.rows?.map{ r -> r.filterIndexed{ i, _ -> keepNew?.get(i) ?: false}}

            val mergedHeaders: List<Header>? = oldImputationTable.headers?.plus(keptNewHeaders ?: emptyList())
            val mergedMatrix: List<List<Boolean?>>? = oldImputationTable.rows?.mapIndexed{ i, row ->  row.plus(keptNewMatrix?.get(i)!!)}
            ImputationTable(mergedHeaders, mergedMatrix)
        } else newImpTable
    }

}