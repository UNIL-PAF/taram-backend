package ch.unil.pafanalysis.analysis.steps.initial_result.spectronaut

import ch.unil.pafanalysis.analysis.model.ColType
import ch.unil.pafanalysis.common.Table


object AdaptSpectronautTable{

    fun adaptTable(table: Table?): Table? {
        val iBaqHeaders = table?.headers?.filter{it.experiment?.field?.equals("ibaq", ignoreCase = true) == true}?.map{it.idx}

        val newCols = table?.cols?.mapIndexed{ i, col ->
            if(iBaqHeaders?.contains(i) == true){
                col.map{ row ->
                    row.toString().split(";").first().toDoubleOrNull() ?: Double.NaN
                }
            }else col
        }

        val newHeaders = table?.headers?.map{ header ->
            if(iBaqHeaders?.contains(header.idx) == true){
                header.copy(type = ColType.NUMBER)
            } else header
        }

        return Table(newHeaders, newCols)
    }
}
