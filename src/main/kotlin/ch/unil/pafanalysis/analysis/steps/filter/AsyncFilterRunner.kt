package ch.unil.pafanalysis.analysis.steps.filter

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.WriteTableData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class AsyncFilterRunner() : CommonStep() {

    private val readTableData = ReadTableData()
    private val writeTableData = WriteTableData()

    @Autowired
    val fixFilterRunner: FixFilterComputation? = null

    @Autowired
    val customFilterRunner: CustomFilterComputation? = null

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val filterRes = filterTable(newStep)

            newStep?.copy(
                results = gson.toJson(filterRes),
            )
        }
        tryToRun(funToRun, newStep)
    }

    fun filterTable(
        step: AnalysisStep?
    ): Filter {

        val resType = step?.analysis?.result?.type
        val outputRoot = getOutputRoot()
        val params = gson.fromJson(step?.parameters, FilterParams().javaClass)
        val table = readTableData.getTable(outputRoot + step?.resultTablePath, step?.commonResult?.headers)
        val fltTableFix = fixFilterRunner?.run(table, params, resType)
        val fltTable = customFilterRunner?.run(fltTableFix, params)
        val fltSize = fltTable?.cols?.get(0)?.size
        val nrRowsRemoved = table.cols?.get(0)?.size?.minus(fltSize ?: 0)
        writeTableData?.write(outputRoot + step?.resultTablePath!!, fltTable!!)
        return Filter(fltSize, nrRowsRemoved)
    }

}