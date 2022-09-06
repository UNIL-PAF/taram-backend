package ch.unil.pafanalysis.analysis.steps.group_filter

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.steps.CommonStep
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
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?, paramsString: String?) {
        try {
            val defaultResult = GroupFilter()

            val (filterRes, resultTableHash) = filterTable(
                newStep,
                gson.fromJson(paramsString, GroupFilterParams().javaClass),
                getOutputRoot()
            )
            val stepWithRes = newStep?.copy(
                parameters = paramsString,
                parametersHash = hashComp.computeStringHash(paramsString),
                resultTableHash = resultTableHash,
                results = gson.toJson(defaultResult)
            )
            val oldStep = analysisStepRepository?.findById(oldStepId)
            val newHash = computeStepHash(stepWithRes, oldStep)

            val updatedStep =
                stepWithRes?.copy(
                    status = AnalysisStepStatus.DONE.value,
                    stepHash = newHash,
                    results = gson.toJson(filterRes)
                )
            analysisStepRepository?.saveAndFlush(updatedStep!!)!!
            updateNextStep(updatedStep!!)
        } catch (e: Exception) {
            println("Error in filter asyncRun ${newStep?.id}")
            e.printStackTrace()
            analysisStepRepository?.saveAndFlush(
                newStep!!.copy(
                    status = AnalysisStepStatus.ERROR.value,
                    error = e.message,
                    stepHash = Crc32HashComputations().getRandomHash()
                )
            )
        }
    }

    fun filterTable(
        step: AnalysisStep?,
        params: GroupFilterParams,
        outputRoot: String?,
    ): Triple<GroupFilter, Long, String> {
        val table = readTableData.getTable(outputRoot + step?.resultTablePath, step?.commonResult?.headers)
        val fltTable = fixFilterRunner?.run(table, params, step?.columnInfo)
        val fltSize = fltTable?.cols?.get(0)?.size
        val nrRowsRemoved = table.cols?.get(0)?.size?.minus(fltSize ?: 0)
        writeTableData?.write(outputRoot + step?.resultTablePath!!, fltTable!!)
        val resFileHash = Crc32HashComputations().computeFileHash(File(outputRoot + step?.resultTablePath))
        val resFilePath = step?.resultTablePath!!
        return Triple(GroupFilter(fltSize, nrRowsRemoved), resFileHash, resFilePath)
    }

}