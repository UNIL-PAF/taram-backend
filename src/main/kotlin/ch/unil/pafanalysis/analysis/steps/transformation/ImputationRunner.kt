package ch.unil.pafanalysis.analysis.steps.transformation

import ch.unil.pafanalysis.analysis.steps.StepException
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.random.RandomAdaptor
import org.apache.commons.math3.random.RandomGenerator
import org.apache.commons.math3.random.Well1024a
import org.apache.commons.math3.stat.descriptive.SummaryStatistics
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import org.springframework.stereotype.Service
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.random.asJavaRandom

@Service
class ImputationRunner() {

    val standardDeviation = StandardDeviation()

    fun runImputation(
        ints: List<List<Double>>,
        transformationParams: TransformationParams
    ): Pair<List<List<Double>>, List<List<Boolean>>> {
        val imputedRows = getImputedPos(ints)

        val newInts =  when (transformationParams.imputationType) {
            ImputationType.NONE.value -> ints
            ImputationType.NAN.value -> replaceMissingBy(ints, Double.NaN)
            ImputationType.VALUE.value -> replaceMissingBy(ints, transformationParams.imputationParams?.replaceValue)
            ImputationType.NORMAL.value -> replaceByNormal(ints, transformationParams.imputationParams)
            else -> {
                throw StepException("${transformationParams.imputationType} is not implemented.")
            }
        }

        return Pair(newInts, imputedRows)
    }

    fun getImputedPos(cols: List<List<Double>>): List<List<Boolean>> {
        return cols.fold(emptyList()){ acc, col ->
            if(acc.isEmpty()){
                col.map{ listOf((it.isNaN() || it == 0.0))}
            }else{
                col.mapIndexed{ i, c -> acc[i].plus( c.isNaN() || c == 0.0) }
            }
        }
    }

    fun replaceByNormal(
        ints: List<List<Double>>,
        params: ImputationParams?
    ): List<List<Double>> {
        return ints.map { col ->
            val cleanInts = col.filter { !it.isNaN() && !it.isInfinite() }
            val sd = standardDeviation.evaluate(cleanInts.toDoubleArray())
            val sdCorr = params!!.width!! * sd
            val mean = cleanInts.average() - params!!.downshift!! * sd
            val random: RandomGenerator = Well1024a()
            val normDist = NormalDistribution(random, mean, sdCorr)
            if(params!!.seed != null) random.setSeed(params!!.seed!!)
            col.map { i ->
                if(i.isNaN() || i == 0.0) normDist.sample() else i
            }
        }
    }

    /*
    fun standardDevitation(ints: List<Double>): Double {
        val mean = ints.average()
        val sum = ints.fold(0.0) { acc, d -> (d - mean).pow(2) + acc }
        return sqrt(sum / ints.size)
    }
     */

    fun replaceMissingBy(
        ints: List<List<Double>>,
        replaceValue: Double?
    ): List<List<Double>> {
        if (replaceValue == null) return ints
        return ints.map { col ->
            col.map { i -> if (i.isNaN() || i == 0.0) replaceValue else i }
        }
    }

}