package ch.unil.pafanalysis.analysis.steps.remove_columns

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.service.AnalysisStepRepository
import ch.unil.pafanalysis.analysis.service.TableService
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.ReadImputationTableData
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.WriteTableData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class AsyncRemoveColumnsRunner() : CommonStep() {

    @Autowired
    private val analysisStepRepo: AnalysisStepRepository? = null

    @Autowired
    private var env: Environment? = null

    @Autowired
    private var tableService: TableService? = null

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val params = gson.fromJson(newStep?.parameters, RemoveColumnsParams().javaClass)

            val headersFiltered = filterHeaders(newStep?.commonResult?.headers, params.keepIdxs)
            val numColsFiltered = filterNumCols(newStep?.commonResult?.numericalColumns, headersFiltered)

            newStep?.copy(
                results = gson.toJson(
                    RemoveColumns(
                        headersFiltered?.size,
                        newStep?.commonResult?.headers?.size?.minus(headersFiltered?.size ?: 0)
                    )
                ),
                commonResult = newStep?.commonResult?.copy(headers = headersFiltered, numericalColumns = numColsFiltered)
            )
        }

        tryToRun(funToRun, newStep)
    }

    private fun filterNumCols(numCols: List<String>?, headersFiltered: List<Header>?): List<String>? {
        val expCols = headersFiltered?.filter{ it.experiment != null}?.map{ it.experiment?.field }?.distinct()
        return numCols?.filter{ expCols?.contains(it) ?: false }
    }

    private fun filterHeaders(headers: List<Header>?, keepIdxs: List<Int>?): List<Header>? {
        return headers?.filter{ keepIdxs?.contains(it.idx) ?: false }
    }

}