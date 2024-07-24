package ch.unil.pafanalysis.analysis.steps.umap

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.ExpInfo
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.analysis.steps.pca.PcaParams
import ch.unil.pafanalysis.common.DefaultColors
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
        val (headers, intTable) = readTableData.getDoubleMatrix(
            table,
            field,
            step?.columnInfo?.columnMapping?.experimentDetails
        )
        if (checkIfMissingVals(intTable)) throw StepException("Cannot perform UMAP on missing values.")
        val umaps = rComputeUmap(intTable, params)
        val groupNames: List<String>? =
            if (params?.useAllGroups == false) step?.columnInfo?.columnMapping?.groupsOrdered?.filter {
                params?.selGroups?.contains(
                    it
                ) ?: false
            } else step?.columnInfo?.columnMapping?.groupsOrdered
        val groupColors: List<String> = getColors(params, step?.columnInfo?.columnMapping?.groupsOrdered)

        return createUmapRes(
            umaps,
            headers,
            step?.columnInfo?.columnMapping?.experimentDetails,
            groupNames,
            groupColors
        )
    }

    private fun checkIfMissingVals(intTable: List<List<Double>>?): Boolean {
        return intTable?.find { it.find { it.isNaN() } == null } == null
    }

    private fun getColors(params: UmapParams?, groupsOrderer: List<String>?): List<String> {
        return if (params?.useAllGroups == false) {
            val selIdxs = params?.selGroups?.map { group -> groupsOrderer?.indexOf(group) }
            DefaultColors.plotColors.filterIndexed { i, _ -> selIdxs?.contains(i) ?: false }
        } else {
            DefaultColors.plotColors
        }
    }

    private fun createUmapRes(
        umaps: List<List<Double>>,
        headers: List<Header>,
        expDetails: Map<String, ExpInfo>?,
        groupNames: List<String>?,
        groupColors: List<String>
    ): UmapRes {
        val groups: List<String> = if (groupNames != null && groupNames.isNotEmpty()) groupNames else
            headers.filter { h -> expDetails?.get(h.experiment?.name)?.group != null }
                .map { expDetails?.get(it.experiment?.name)?.group!! }.distinct()

        val groupsDefined: Boolean = expDetails?.values?.any{it.group != null} ?: false

        val umapListOrig = umaps.mapIndexed { i, umap ->
            OneUmapRow(
                groupName = expDetails?.get(headers?.get(i).experiment?.name)?.group,
                expName = headers?.get(i).experiment?.name,
                umapVals = umap
            )
        }

        val umapList = if(groupsDefined) umapListOrig.filter{it.groupName != null} else umapListOrig

        val existingGroups = groups.filter{a -> umapList.find{it.groupName == a} != null}

        return UmapRes(groups = existingGroups, nrUmaps = umaps.size, umapList = umapList, groupColors = groupColors)
    }

    private fun rComputeUmap(ints: List<List<Double>>, params: UmapParams?): List<List<Double>> {
        val code = RCode.create()
        code.addDoubleMatrix("m", ints.map { it.toDoubleArray() }.toTypedArray())
        code.addRCode("library(umap)")
        code.addRCode("umap_res <- umap(m, n_neighbors=${params?.nrOfNeighbors ?: 2}, min_dist=${params?.minDistance ?: 0.5})")
        code.addRCode("res <- list(umaps=umap_res\$layout)")
        val caller = RCaller.create(code, RCallerOptions.create())
        caller.runAndReturnResult("res")
        val umaps = caller.parser.getAsDoubleMatrix("umaps").map { it.toList() }
        return umaps
    }

}