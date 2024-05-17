package ch.unil.pafanalysis.analysis.steps.umap

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.ExpInfo
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import com.github.rcaller.rstuff.RCaller
import com.github.rcaller.rstuff.RCallerOptions
import com.github.rcaller.rstuff.RCode
import org.springframework.stereotype.Service

@Service
class UmapComputation {
    private val readTableData = ReadTableData()

    fun run(table: Table?, params: UmapParams?, step: AnalysisStep?): UmapRes {
        val field = params?.column ?: step?.columnInfo?.columnMapping?.intCol
        val (headers, intTable) = readTableData.getDoubleMatrix(table, field, step?.columnInfo?.columnMapping?.experimentDetails)
        if(checkIfMissingVals(intTable)) throw StepException("Cannot perform UMAP on missing values.")
        val umaps = rComputeUmap(intTable)
        val groupNames: List<String>? = step?.columnInfo?.columnMapping?.groupsOrdered
        return createUmapRes(umaps, headers, step?.columnInfo?.columnMapping?.experimentDetails, groupNames)
    }

    private fun checkIfMissingVals(intTable: List<List<Double>>?): Boolean {
        return intTable?.find { it.find{ it.isNaN()} == null } == null
    }

    private fun createUmapRes(umaps: List<List<Double>>, headers: List<Header>, expDetails: Map<String, ExpInfo>?, groupNames: List<String>?): UmapRes {
        val groups: List<String> = if(groupNames != null && groupNames.isNotEmpty()) groupNames else
            headers.filter { h -> expDetails?.get(h.experiment?.name)?.group != null }.map { expDetails?.get(it.experiment?.name)?.group!! }.distinct()

        val umapList = umaps.mapIndexed { i, umap ->
            OneUmapRow(
                groupName = expDetails?.get(headers?.get(i).experiment?.name)?.group,
                expName = headers?.get(i).experiment?.name,
                umapVals = umap
            )
        }
        return UmapRes(groups = groups, nrUmaps = umaps.size, umapList = umapList)
    }

    private fun rComputeUmap(ints: List<List<Double>>): List<List<Double>> {
        val code = RCode.create()
        code.addDoubleMatrix("m", ints.map { it.toDoubleArray() }.toTypedArray())
        code.addRCode("library(umap)")
        code.addRCode("umap_res <- umap(m, n_neighbors=2)")
        code.addRCode("res <- list(umaps=umap_res\$layout)")
        val caller = RCaller.create(code, RCallerOptions.create())
        caller.runAndReturnResult("res")
        val umaps = caller.parser.getAsDoubleMatrix("umaps").map { it.toList() }
        return umaps
    }

}