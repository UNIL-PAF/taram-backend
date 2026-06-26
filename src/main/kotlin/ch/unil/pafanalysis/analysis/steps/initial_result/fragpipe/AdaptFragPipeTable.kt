package ch.unil.pafanalysis.analysis.steps.initial_result.fragpipe

import ch.unil.pafanalysis.common.Table

object AdaptFragPipeTable {

    fun adaptTable(table: Table?): Table {
        val newHeaders = table?.headers?.map{ header ->
           val newExp = header.experiment?.copy(field=header.experiment.field?.replace(" ", "."))
           header.copy(experiment = newExp)
        }
        return Table(newHeaders, table?.cols)
    }
}
