package ch.unil.pafanalysis.analysis.steps.log_transformation

import ch.unil.pafanalysis.analysis.steps.StepException
import org.springframework.stereotype.Service
import kotlin.math.ln

@Service
class LogTransformationComputation() {

    fun runTransformation(
        ints: List<List<Double>>,
        params: LogTransformationParams
    ): List<List<Double>> {

        fun transformList(intList: List<Double>): List<Double> {
            val a: List<Double> = when (params.transformationType) {
                TransformationType.NONE.value -> intList
                TransformationType.LOG2.value -> {
                    val newList = intList.map { i ->
                        if (i == 0.0) {
                            Double.NaN
                        } else {
                            ln(i) / ln(2.0)
                        }
                    }
                    newList
                }
                else -> {
                    throw StepException("${params.transformationType} is not implemented.")
                }
            }
            return a
        }

        return ints.map { orig: List<Double> ->
            transformList(orig)
        }
    }

}