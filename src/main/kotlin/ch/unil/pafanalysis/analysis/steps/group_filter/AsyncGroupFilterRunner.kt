package ch.unil.pafanalysis.analysis.steps.group_filter

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.filter.FilterParams
import ch.unil.pafanalysis.common.Crc32HashComputations
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.WriteTableData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.File

@Service
class AsyncGroupFilterRunner() : CommonStep() {

    private val readTableData = ReadTableData()
    private val writeTableData = WriteTableData()

    @Autowired
    val fixFilterRunner: FixGroupFilterRunner? = null

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
    ): GroupFilter {
        val outputRoot = getOutputRoot()
        val params = gson.fromJson(step?.parameters, GroupFilterParams().javaClass)
        val table = readTableData.getTable(outputRoot + step?.resultTablePath, step?.commonResult?.headers)
        val fltTable = fixFilterRunner?.run(table, params, step?.columnInfo)
        val fltSize = fltTable?.cols?.get(0)?.size
        val nrRowsRemoved = table.cols?.get(0)?.size?.minus(fltSize ?: 0)
        writeTableData?.write(outputRoot + step?.resultTablePath!!, fltTable!!)
        return GroupFilter(fltSize, nrRowsRemoved)
    }

}