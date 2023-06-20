package ch.unil.pafanalysis.analysis.steps.order_columns

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.CommonStep
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class AsyncOrderColumnsRunner() : CommonStep() {

    @Autowired
    private var env: Environment? = null

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val params = gson.fromJson(newStep?.parameters, OrderColumnsParams().javaClass)

            val groupHeaders = addGroupNames(newStep?.commonResult?.headers, params.addGroupsToHeaders)

            /*
            val headersFiltered = filterHeaders(newStep?.commonResult?.headers, params.keepIdxs)

            val newHeaders = headersFiltered?.mapIndexed{ i, h -> h.copy(idx = i) }

            val oldTable = ReadTableData().getTable(
                env?.getProperty("output.path").plus(newStep?.resultTablePath),
                newStep?.commonResult?.headers
            )

            val newTable = Table(headers = newHeaders, cols = oldTable.cols?.filterIndexed{ i, _ -> params.keepIdxs?.contains(i) ?: false})

            WriteTableData().write(
                env?.getProperty("output.path")?.plus(newStep?.resultTablePath)!!,
                newTable
            )
          */

            newStep?.copy(
                results = gson.toJson(
                    OrderColumns(
                    )
                ),
                commonResult = newStep?.commonResult?.copy(headers = groupHeaders)
            )
        }

        tryToRun(funToRun, newStep)
    }

    private fun addGroupNames(headers: List<Header>?, addGroupsToHeader: Boolean?): List<Header>?{
        return headers
    }

}