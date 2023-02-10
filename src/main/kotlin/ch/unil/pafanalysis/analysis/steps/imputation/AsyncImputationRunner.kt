package ch.unil.pafanalysis.analysis.steps.imputation

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class AsyncImputationRunner() : CommonStep() {

    private val readTableData = ReadTableData()
    private val writeTableData = WriteTableData()
    private val writeImputation = WriteImputationTableData()

    @Autowired
    val imputationRunner: ImputationComputation? = null

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val params = gson.fromJson(newStep?.parameters, ImputationParams().javaClass)

            val (nrImputed, imputationFileName) = transformTable(
                newStep,
                params
            )

            newStep?.copy(
                results = gson.toJson(Imputation(nrImputed)),
                imputationTablePath = imputationFileName
            )
        }

        tryToRun(funToRun, newStep)
    }

    fun transformTable(
        step: AnalysisStep?,
        params: ImputationParams
    ): Pair<Int?, String?> {
        val intCol = params.intCol ?: step?.columnInfo?.columnMapping?.intCol
        val table = readTableData.getTable(getOutputRoot() + step?.resultTablePath, step?.commonResult?.headers)
        val (selHeaders, ints) = readTableData.getDoubleMatrix(table, intCol)
        val (impInts, imputedRows) = imputationRunner!!.runImputation(ints, params)

        val newCols: List<List<Any>>? = table.cols?.mapIndexed { i, c ->
            val selHeader = selHeaders.withIndex().find { it.value.idx == i }
            if (selHeader != null) {
                impInts[selHeader.index]
            } else c
        }

        writeTableData.write(getOutputRoot() + step?.resultTablePath, table.copy(cols = newCols))

        return if (imputedRows != null) {
            val imputationTable = ImputationTable(selHeaders, imputedRows)
            val imputationFileName = step?.resultTablePath?.replace(".txt", "_imputation.txt")
            writeImputation.write(getOutputRoot() + imputationFileName, imputationTable)
            val nrImputed: Int = imputedRows.sumOf { r -> r.sumOf { (if (it) 1 else 0).toInt() } }
            Pair(nrImputed, imputationFileName)
        } else Pair(null, null)

    }

}