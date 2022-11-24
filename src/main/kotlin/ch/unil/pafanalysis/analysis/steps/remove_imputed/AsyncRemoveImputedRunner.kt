package ch.unil.pafanalysis.analysis.steps.remove_imputed

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.service.AnalysisStepRepository
import ch.unil.pafanalysis.analysis.service.TableService
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.ReadImputationTableData
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.WriteImputationTableData
import ch.unil.pafanalysis.common.WriteTableData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class AsyncRemoveImputedRunner() : CommonStep() {

    @Autowired
    private val analysisStepRepo: AnalysisStepRepository? = null

    @Autowired
    private var env: Environment? = null

    @Autowired
    private var tableService: TableService? = null

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val params = gson.fromJson(newStep?.parameters, RemoveImputedParams().javaClass)
            val stepBefore = analysisStepRepo?.findById(newStep?.beforeId!!)

            val table = ReadTableData().getTable(
                env?.getProperty("output.path").plus(stepBefore?.resultTablePath),
                stepBefore?.commonResult?.headers
            )

            val (nrImputed, nrImputedRows) = if (stepBefore?.imputationTablePath != null) {
                val imputed = ReadImputationTableData().getTable(
                    env?.getProperty("output.path").plus(stepBefore?.imputationTablePath),
                    stepBefore?.commonResult?.headers
                )
                val imputedRepl = tableService?.replaceImputedVals(table, imputed, params.replaceBy?.value!!)
                WriteTableData().write(
                    env?.getProperty("output.path")?.plus(newStep?.resultTablePath)!!,
                    imputedRepl?.first!!
                )
                Pair(imputedRepl.second, imputedRepl.third)
            } else {
                Pair(0, 0)
            }

            newStep?.copy(
                results = gson.toJson(RemoveImputed(nrImputed, nrImputedRows)),
                imputationTablePath = null
            )
            newStep
        }

        tryToRun(funToRun, newStep)
    }
}