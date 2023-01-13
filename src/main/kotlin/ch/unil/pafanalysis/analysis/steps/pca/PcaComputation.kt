package ch.unil.pafanalysis.analysis.steps.pca

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.Header
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
        val (headers, table) = readTableData.getDoubleMatrix(table, field)
        val (pcs, explVar) = rComputePc(table)

        return createPcaRes(pcs, explVar, headers)
    }

    private fun createPcaRes(pcs: List<List<Double>>, explVar: List<Double>, headers: List<Header>): PcaRes {
        val groups: List<String> =
            headers.filter { h -> h.experiment?.group != null }.map { it.experiment?.group!! }.distinct()
        val pcList = pcs.mapIndexed { i, pc ->
            OnePcRow(
                groupName = headers?.get(i).experiment?.group,
                expName = headers?.get(i).experiment?.name,
                pcVals = pc
            )
        }
        return PcaRes(groups = groups, nrPc = explVar.size, explVars = explVar, pcList = pcList)
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