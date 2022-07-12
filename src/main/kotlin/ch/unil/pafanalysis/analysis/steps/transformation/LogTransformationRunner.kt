package ch.unil.pafanalysis.analysis.steps.transformation

import ch.unil.pafanalysis.analysis.steps.StepException
import org.springframework.stereotype.Service
import kotlin.math.ln

@Service
class LogTransformationRunner() {

    fun runTransformation(
        ints: List<Pair<String, List<Double>>>,
        transformationParams: TransformationParams
    ): List<Pair<String, List<Double>>> {

        fun transformList(intList: List<Double>): List<Double> {
            val a: List<Double> = when (transformationParams.transformationType) {
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
                    throw StepException("${transformationParams.normalizationType} is not implemented.")
                }
            }
            return a
        }

        return ints.map { (name, orig: List<Double>) ->
            Pair(name, transformList(orig))
        }
    }

}