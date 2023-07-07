package ch.unil.pafanalysis.analysis.steps.rename_columns

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.common.WriteTableData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class AsyncRenameColumnsRunner() : CommonStep() {

    @Autowired
    private var env: Environment? = null

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val params = gson.fromJson(newStep?.parameters, RenameColumnsParams().javaClass)

            val origTable = ReadTableData().getTable(
                env?.getProperty("output.path").plus(newStep?.resultTablePath),
                newStep?.commonResult?.headers
            )

            val renamed1 = addConditionNames(origTable, params?.addConditionNames)

            WriteTableData().write(
                env?.getProperty("output.path")?.plus(newStep?.resultTablePath)!!,
                origTable!!
            )

            newStep?.copy(
                results = gson.toJson(
                    RenameColumns()
                ),
                commonResult = newStep?.commonResult?.copy(headers = renamed1?.headers)
            )
        }

        tryToRun(funToRun, newStep)
    }

    private fun addConditionNames(table: Table?, addConditionNames: Boolean?): Table? {
        return table
    }

}