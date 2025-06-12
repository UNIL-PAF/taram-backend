package ch.unil.pafanalysis.analysis.steps.pca

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.ReadTableData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class AsyncPcaRunner() : CommonStep() {

    @Autowired
    private val pcaComputation: PcaComputation? = null

    private val readTableData = ReadTableData()

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val pcaRes = createPcaObj(newStep)
            newStep?.copy(
                results = gson.toJson(pcaRes),
            )
        }
        tryToRun(funToRun, newStep)
    }

    private fun createPcaObj(analysisStep: AnalysisStep?): PcaRes? {
        val params = gson.fromJson(analysisStep?.parameters, PcaParams().javaClass)

        val table = readTableData.getTable(
            getOutputRoot().plus(analysisStep?.resultTablePath),
            analysisStep?.commonResult?.headers
        )
        return pcaComputation?.run(table, params, analysisStep)
    }

}