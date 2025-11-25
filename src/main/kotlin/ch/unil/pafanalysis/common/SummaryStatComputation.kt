package ch.unil.pafanalysis.common

import ch.unil.pafanalysis.analysis.model.ExpInfo
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.summary_stat.SummaryStat
import com.google.common.math.Quantiles
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import kotlin.math.sqrt

class SummaryStatComputation {
    private fun addSummaryStat(h: Header, ints: List<Double>, old: SummaryStat, expDetails: Map<String, ExpInfo>?): SummaryStat {
        val expName = h.experiment?.name
        val groupName = expDetails?.get(h.experiment?.name)?.group
        val nrValid = ints.filter { !it.isNaN() }.size
        val nrNaN = ints.filter { it.isNaN() }.size
        val fltInts = ints.filter { !it.isNaN() }
        val min = fltInts.minOrNull()
        val max = fltInts.maxOrNull()
        val mean = fltInts.average()
        val median = Quantiles.median().compute(fltInts)
        val stdDev = StandardDeviation().evaluate(fltInts.toDoubleArray())
        val sum = fltInts.sum()
        val stdErr = stdDev / sqrt(fltInts.size.toDouble())
        val coefOfVar = stdDev / mean

        return old.copy(
            expNames = if(expName != null) (old.expNames?: emptyList()).plus(expName) else old.expNames,
            groups = if(groupName != null) (old.groups?: emptyList()).plus(groupName) else old.groups,
            nrValid = (old.nrValid?: emptyList()).plus(nrValid),
            nrNaN = (old.nrNaN?: emptyList()).plus(nrNaN),
            min = if(min != null) (old.min?: emptyList()).plus(min) else old.min,
            max = if(max != null) (old.max?: emptyList()).plus(max) else old.max,
            mean = if(max != null) (old.mean?: emptyList()).plus(mean) else old.mean,
            median = (old.median?: emptyList()).plus(median),
            stdDev = (old.stdDev?: emptyList()).plus(stdDev),
            sum = (old.sum?: emptyList()).plus(sum),
            stdErr = (old.stdErr?: emptyList()).plus(stdErr),
            coefOfVar = (old.coefOfVar?: emptyList()).plus(coefOfVar),
        )
    }

    fun getSummaryStat(intMatrix: List<List<Double>>, headers: List<Header>, expDetails: Map<String, ExpInfo>?): SummaryStat {
        val zipped = headers.zip(intMatrix)
        val summaryStat = zipped.fold(SummaryStat()) { acc, el ->
            val (h, ints) = el
            addSummaryStat(h, ints, acc, expDetails)
        }
        return summaryStat
    }

    fun getBasicSummaryStat(intMatrix: List<List<Double>>, headers: List<Header>): SummaryStat {
        val flatInts = intMatrix.flatten()
        val nrNaN = flatInts.filter { it.isNaN() }.size
        val nrValid = flatInts.filter { ! it.isNaN() }.size
        val fltInts = flatInts.filter { !it.isNaN() }
        val min = fltInts.minOrNull()
        val max = fltInts.maxOrNull()
        val mean = fltInts.average()
        val median = Quantiles.median().compute(fltInts)
        val sum = fltInts.sum()

        return SummaryStat(
            min = if(min!=null) listOf(min) else null,
            max = if(max!=null) listOf(max) else null,
            mean = listOf(mean),
            median = listOf(median),
            sum = listOf(sum),
            nrNaN = listOf(nrNaN),
            nrValid = listOf(nrValid),
        )
    }

}