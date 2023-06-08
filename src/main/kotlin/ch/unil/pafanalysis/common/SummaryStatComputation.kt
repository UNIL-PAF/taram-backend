package ch.unil.pafanalysis.common

import ch.unil.pafanalysis.analysis.model.ExpInfo
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.summary_stat.SummaryStat
import com.google.common.math.Quantiles
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import org.apache.commons.math3.stat.descriptive.summary.Sum
import kotlin.math.sign
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
            median = if(median != null) (old.median?: emptyList()).plus(median) else old.median,
            stdDev = if(stdDev != null) (old.stdDev?: emptyList()).plus(stdDev) else old.stdDev,
            sum = if(sum != null) (old.sum?: emptyList()).plus(sum) else old.sum,
            stdErr = if(stdErr != null) (old.stdErr?: emptyList()).plus(stdErr) else old.stdErr,
            coefOfVar = if(coefOfVar != null) (old.coefOfVar?: emptyList()).plus(coefOfVar) else old.coefOfVar,
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

        println(nrNaN)

        return SummaryStat(
            min = if(min!=null) listOf(min) else null,
            max = if(max!=null) listOf(max) else null,
            mean = if(mean!=null) listOf(mean) else null,
            median = if(median!=null) listOf(median) else null,
            sum = if(sum!=null) listOf(sum) else null,
            nrNaN = if(nrNaN!=null) listOf(nrNaN) else null,
            nrValid = if(nrValid!=null) listOf(nrValid) else null
        )
    }

}