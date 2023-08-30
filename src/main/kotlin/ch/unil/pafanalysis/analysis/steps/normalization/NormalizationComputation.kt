package ch.unil.pafanalysis.analysis.steps.normalization

import ch.unil.pafanalysis.analysis.steps.StepException
import com.google.common.math.Quantiles
import org.springframework.stereotype.Service

@Service
class NormalizationComputation() {

    fun runNormalization(
        ints: List<List<Double>>,
        params: NormalizationParams
    ): List<List<Double>> {
        if (params.normalizationType == NormalizationType.NONE.value) return ints

        val myNumber = when (params.normalizationType) {
            NormalizationType.MEDIAN.value -> fun(orig: List<Double>): Double {
                return Quantiles.median().compute(orig)
            }
            NormalizationType.MEAN.value -> fun(orig: List<Double>): Double { return orig.average() }
            else -> {
                throw StepException("${params.normalizationType} is not implemented.")
            }
        }

        return ints.map { orig: List<Double> ->
            val noNaNs = orig.filter { !it.isNaN() }
            when (params.normalizationCalculation) {
                NormalizationCalculation.DIVISION.value -> orig.map { it - myNumber(noNaNs) }
                NormalizationCalculation.SUBSTRACTION.value -> orig.map { it - myNumber(noNaNs) }
                else -> throw StepException("${params.normalizationCalculation} is not implemented.")
            }
        }
    }


}