package ch.unil.pafanalysis.analysis.steps.rename_columns

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.ExpInfo
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.common.WriteTableData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import kotlin.math.exp

@Service
class AsyncRenameColumnsRunner() : CommonStep() {

    @Autowired
    private var env: Environment? = null

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val params = gson.fromJson(newStep?.parameters, RenameColumnsParams().javaClass)

            if(params?.rename?.find{ r -> r.idx == newStep?.columnInfo?.columnMapping?.intCol} != null){
                throw StepException("You cannot rename the intensity column [${newStep?.columnInfo?.columnMapping?.intCol}] that is currently used.")
            }

            val origTable = ReadTableData().getTable(
                env?.getProperty("output.path").plus(newStep?.resultTablePath),
                newStep?.commonResult?.headers
            )
            val renamed1 = renameItems(origTable, params?.rename)
            val renamed2 = addConditionNames(renamed1, params?.addConditionNames, newStep?.columnInfo?.columnMapping?.experimentDetails)

            WriteTableData().write(
                env?.getProperty("output.path")?.plus(newStep?.resultTablePath)!!,
                renamed2!!
            )

            newStep?.copy(
                results = gson.toJson(
                    RenameColumns()
                ),
                commonResult = newStep?.commonResult?.copy(headers = renamed2?.headers)
            )
        }

        tryToRun(funToRun, newStep)
    }

    private fun addConditionNames(table: Table?, addConditionNames: Boolean?, expInfo: Map<String, ExpInfo>?): Table? {
        return if(addConditionNames == true){
            val newHeaders = table?.headers?.map{ h ->
                if(h.experiment != null){
                    val groupName = expInfo?.get(h.experiment.name)?.group
                    val newName = if(groupName != null) h.name + "." + groupName else h.name
                    h.copy(name = newName)
                } else h
            }
            table?.copy(headers = newHeaders)
        }else table
    }

    private fun renameItems(table: Table?, rename: List<RenameCol>?): Table? {
        val newHeaders = table?.headers?.map{ h ->
            if(h.experiment != null){
                val renameItem = rename?.find{it.idx == h.experiment.field}
                val newExpField = if(renameItem != null) renameItem.name else h.experiment.field
                val newExp = h.experiment.copy(field = newExpField)
                val newName = if(renameItem != null) h.experiment.name + "." + renameItem.name else h.name
                h.copy(name = newName, experiment = newExp)
            }else{
                val renameItem = rename?.find{it.idx?.toInt() == h.idx}
                val newName = if(renameItem != null) renameItem.name else h.name
                h.copy(name = newName)
            }
        }
        return table?.copy(headers = newHeaders)
    }

}