package ch.unil.pafanalysis.analysis.steps.transformation

import ch.unil.pafanalysis.analysis.steps.StepException
import com.google.common.math.Quantiles
import org.springframework.stereotype.Service

@Service
class NormalizationRunner() {

    fun runNormalization(
        ints: List<Pair<String, List<Double>>>,
        transformationParams: TransformationParams
    ): List<Pair<String, List<Double>>> {
        val subtract = when (transformationParams.normalizationType) {
            NormalizationType.MEDIAN.value -> fun(orig: List<Double>): Double {
                return Quantiles.median().compute(orig)
            }
            NormalizationType.MEAN.value -> fun(orig: List<Double>): Double { return orig.average() }
            else -> {
                throw StepException("${transformationParams.normalizationType} is not implemented.")
            }
        }
        return ints.map { (name, orig: List<Double>) ->
            val noNaNs = orig.filter { !it.isNaN() }
            Pair(name, orig.map { it - subtract(noNaNs) })
        }
    }


}