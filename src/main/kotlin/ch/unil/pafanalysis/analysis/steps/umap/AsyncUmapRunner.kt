package ch.unil.pafanalysis.analysis.steps.umap

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.ExpInfo
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import com.google.common.math.Quantiles
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import kotlin.math.log2

@Service
class AsyncUmapRunner() : CommonStep() {

    @Autowired
    private val umapComputation: UmapComputation? = null

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

    private fun createPcaObj(analysisStep: AnalysisStep?): UmapRes? {
        val params = gson.fromJson(analysisStep?.parameters, UmapParams().javaClass)

        val table = readTableData.getTable(
            getOutputRoot().plus(analysisStep?.resultTablePath),
            analysisStep?.commonResult?.headers
        )
        return umapComputation?.run(table, params, analysisStep)
    }

}