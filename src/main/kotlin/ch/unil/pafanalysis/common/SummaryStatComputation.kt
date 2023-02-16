package ch.unil.pafanalysis.common

import ch.unil.pafanalysis.analysis.steps.summary_stat.SummaryStat
import com.google.common.math.Quantiles

class SummaryStatComputation {
    fun getSummaryStat(ints: List<List<Double>>): SummaryStat {
        val flatInts = ints.flatten()
        val nrNans = flatInts.filter{it.isNaN()}.size
        val fltInts = flatInts.filter{! it.isNaN()}
        val min =  fltInts.minOrNull()
        val max = fltInts.maxOrNull()
        val mean = fltInts.average()
        val median = Quantiles.median().compute(fltInts)
        return SummaryStat(min, max, mean, median, nrNans)
    }
}