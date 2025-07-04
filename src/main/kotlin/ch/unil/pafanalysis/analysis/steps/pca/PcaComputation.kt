package ch.unil.pafanalysis.analysis.steps.pca

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.ColumnMapping
import ch.unil.pafanalysis.analysis.model.ExpInfo
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.DefaultColors
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import com.github.rcaller.rstuff.RCaller
import com.github.rcaller.rstuff.RCallerOptions
import com.github.rcaller.rstuff.RCode
import org.springframework.stereotype.Service

@Service
class PcaComputation {
    private val readTableData = ReadTableData()

    fun run(table: Table?, params: PcaParams?, step: AnalysisStep?): PcaRes {
        val field = params?.column ?: step?.columnInfo?.columnMapping?.intCol

        val (headers, intTable) = readTableData.getDoubleMatrix(
            table,
            field,
            step?.columnInfo?.columnMapping?.experimentDetails
        )

        if (checkIfMissingVals(intTable)) throw StepException("Cannot perform PCA on missing values.")

        val fltHeaders = if(params?.useAllGroups == false) fltHeadersByGroup(params.selGroups, headers, step?.columnInfo?.columnMapping) else headers
        val fltIntTable = if(params?.useAllGroups == false) readTableData.getDoubleMatrix(table, fltHeaders) else intTable

        val (pcs, explVar) = rComputePc(fltIntTable)
        val groupNames: List<String>? =
            if (params?.useAllGroups == false) step?.columnInfo?.columnMapping?.groupsOrdered?.filter{ params.selGroups?.contains(it) ?: false} else step?.columnInfo?.columnMapping?.groupsOrdered
        val groupColors: List<String>? = getColors(params, step?.columnInfo?.columnMapping?.groupsOrdered, step?.columnInfo?.columnMapping?.experimentDetails)

        return createPcaRes(
            pcs,
            explVar,
            fltHeaders,
            step?.columnInfo?.columnMapping?.experimentDetails,
            groupNames,
            groupColors
        )
    }

    private fun fltHeadersByGroup(selGroups: List<String>?, headers: List<Header>, columnMapping: ColumnMapping?): List<Header> {
        val expInfos = columnMapping?.experimentDetails?.values
        return headers.filter { header ->
            selGroups?.contains(expInfos?.find{it.name == header.experiment?.name}?.group) ?: false
        }
    }

    private fun getColors(params: PcaParams?, groupsOrderer: List<String>?, experimentDetails: Map<String, ExpInfo>?): List<String>? {
        val defaultColors =  if(params?.useAllGroups == false){
            val selIdxs =  params.selGroups?.map{ group -> groupsOrderer?.indexOf(group)}
            DefaultColors.plotColors.filterIndexed{ i, _ -> selIdxs?.contains(i) ?: false}
        }else{
            DefaultColors.plotColors
        }

        val orderedSelGroups = if(params?.useAllGroups == false) groupsOrderer?.filter{params.selGroups?.contains(it) == true} else groupsOrderer
        return orderedSelGroups?.zip(defaultColors)?.map{(groupName, color) ->
            experimentDetails?.entries?.find{ it.value.group == groupName }?.value?.color ?: color
        }
    }

    private fun checkIfMissingVals(intTable: List<List<Double>>?): Boolean {
        return intTable?.find { it.find { it.isNaN() } == null } == null
    }

    private fun createPcaRes(
        pcs: List<List<Double>>,
        explVar: List<Double>,
        headers: List<Header>,
        expDetails: Map<String, ExpInfo>?,
        groupNames: List<String>?,
        groupColors: List<String>?
    ): PcaRes {
        val groups: List<String> = if (groupNames != null && groupNames.isNotEmpty()) groupNames else
            headers.filter { h -> expDetails?.get(h.experiment?.name)?.group != null }
                .map { expDetails?.get(it.experiment?.name)?.group!! }.distinct()

        val groupsDefined: Boolean = expDetails?.values?.any{it.group != null} ?: false

        val pcListOrig = pcs.mapIndexed { i, pc ->
            OnePcRow(
                groupName = expDetails?.get(headers[i].experiment?.name)?.group,
                expName = headers[i].experiment?.name,
                pcVals = pc
            )
        }
        val pcList = if(groupsDefined) pcListOrig.filter{it.groupName != null} else pcListOrig
        val existingGroups = groups.filter{a -> pcList.find{it.groupName == a} != null}
        return PcaRes(groups = existingGroups, nrPc = explVar.size, explVars = explVar, pcList = pcList, groupColors = groupColors)
    }

    private fun rComputePc(ints: List<List<Double>>): Pair<List<List<Double>>, List<Double>> {
        val code = RCode.create()
        code.addDoubleMatrix("m", ints.map { it.toDoubleArray() }.toTypedArray())
        code.addRCode("pr_res <- prcomp(m)")
        code.addRCode("sdev_sum <- 100/sum(pr_res\$sdev^2)")
        code.addRCode("expl_var <- pr_res\$sdev^2*sdev_sum")
        code.addRCode("res <- list(pcs=pr_res\$x, expl_var=expl_var)")
        val caller = RCaller.create(code, RCallerOptions.create())
        caller.runAndReturnResult("res")
        val pcs = caller.parser.getAsDoubleMatrix("pcs").map { it.toList() }
        val explVar = caller.parser.getAsDoubleArray("expl_var").toList()
        return Pair(pcs, explVar)
    }

}