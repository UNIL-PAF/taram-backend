package ch.unil.pafanalysis.common

import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.summary_stat.OneGroupSummary
import ch.unil.pafanalysis.analysis.steps.summary_stat.SummaryStat
import com.google.common.math.Quantiles
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import kotlin.math.sqrt

class SummaryStatComputation {
    fun getSummaryStat(intMatrix: List<List<Double>>, headers: List<Header>): SummaryStat {
        val zipped = headers.zip(intMatrix)
        val statList = zipped.map { (h, ints) ->
            val expName = h.experiment?.name
            val groupName = h.experiment?.group
            val nrValid = ints.filter { !it.isNaN() }.size
            val fltInts = ints.filter { !it.isNaN() }
            val min = fltInts.minOrNull()
            val max = fltInts.maxOrNull()
            val mean = fltInts.average()
            val median = Quantiles.median().compute(fltInts)
            val stdDev = StandardDeviation().evaluate(fltInts.toDoubleArray())
            val sum = fltInts.sum()
            val stdErr = stdDev / sqrt(fltInts.size.toDouble())
            val coeffOfVar = stdDev / mean
            OneGroupSummary(
                expName = expName,
                groupName = groupName,
                min = min,
                max = max,
                nrValid = nrValid,
                mean = mean,
                median = median,
                sum = sum,
                stdDev = stdDev,
                stdErr = stdErr,
                coeffOfVar = coeffOfVar
            )
        }
        return SummaryStat(statList)
    }
}