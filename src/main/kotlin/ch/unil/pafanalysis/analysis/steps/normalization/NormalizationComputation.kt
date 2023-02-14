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

        val subtract = when (params.normalizationType) {
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
            orig.map { it - subtract(noNaNs) }
        }
    }


}