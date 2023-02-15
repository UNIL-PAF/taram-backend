package ch.unil.pafanalysis.common

import com.google.common.math.Quantiles

class SummaryStatComputation {

    data class SummaryStat(val min: Double?, val max: Double?, val mean: Double?, val median: Double?, val nrNans: Int?)

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