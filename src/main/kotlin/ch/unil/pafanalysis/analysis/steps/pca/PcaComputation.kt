package ch.unil.pafanalysis.analysis.steps.pca

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import com.github.rcaller.rstuff.RCaller
import com.github.rcaller.rstuff.RCallerOptions
import com.github.rcaller.rstuff.RCode
import org.springframework.stereotype.Service
import kotlin.math.exp

/*
https://github.com/vqv/ggbiplot/blob/master/R/ggbiplot.r
pca_res <- prcomp(t(pg_6[,lfq_cols]))
pca_res$sdev[2]^2/sum(pca_res$sdev^2) * 100

sdev_sum <- 100/sum(pca_res$sdev^2)
expl_var <- pca_res$sdev^2*sdev_sum
res <- list(pcs = predict(pca_res), expl_var = expl_var)
 */

@Service
class PcaComputation {
    private val readTableData = ReadTableData()

    fun run(table: Table?, params: PcaParams?, step: AnalysisStep?): PcaRes {
        val field = params?.field ?: step?.columnInfo?.columnMapping?.intCol
        val m: Pair<List<Header>, List<List<Double>>> = readTableData.getDoubleMatrix(table, field)
        val (pcs, explVar) = rComputePc(m.second)
        return createPcaRes(pcs, explVar).copy(expNames = step?.columnInfo?.columnMapping?.experimentNames)
    }

    private fun createPcaRes(pcs: List<List<Double>>, explVar: List<Double>, groups: Map<String, List<Int>>? = null): PcaRes {
        val myGroups = groups ?: mapOf<String, List<Int>>("" to explVar.indices.toList())
        val groups: List<PcGroup> = myGroups.map{group -> PcGroup(groupName = group.key, pcList = getOnePcList(group.value, pcs, explVar))}
        return PcaRes(groups = groups, nrPc = explVar.size)
    }

    private fun getOnePcList(groupIdxs: List<Int>, pcs: List<List<Double>>, explVar: List<Double>): List<OnePc> {
        return pcs.mapIndexed{i, pc -> OnePc(i+1, pcVals = pc.filterIndexed{k, _ ->
            groupIdxs.contains(k) },
            explVar = explVar[i] )}
    }

    private fun rComputePc(ints: List<List<Double>>): Pair<List<List<Double>>, List<Double>> {
        val code = RCode.create()
        code.addDoubleMatrix("m", ints.map{it.toDoubleArray()}.toTypedArray())
        code.addRCode("pr_res <- prcomp(t(m))")
        code.addRCode("sdev_sum <- 100/sum(pr_res\$sdev^2)")
        code.addRCode("expl_var <- pr_res\$sdev^2*sdev_sum")
        code.addRCode("res <- list(pcs=t(predict(pr_res)), expl_var=expl_var)")
        val caller = RCaller.create(code, RCallerOptions.create())
        caller.runAndReturnResult("res")
        val pcs = caller.parser.getAsDoubleMatrix("pcs").map{it.toList()}
        val explVar = caller.parser.getAsDoubleArray("expl_var").toList()
        return Pair(pcs, explVar)
    }




}