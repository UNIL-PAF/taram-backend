package ch.unil.pafanalysis.analysis.steps.scatter_plot

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.math.RoundingMode

@SpringBootTest
class ComputeRegressionTests {

    private val ROUNDING_PRECISION = 4

    val computeRegression = ComputeRegression()

    @Test
    fun computeRegression() {
        val xList = listOf(151, 174, 138, 186, 128, 136, 179, 163, 152, 131).map{it.toDouble()}
        val yList = listOf(63, 81, 56, 91, 47, 57, 76, 72, 62, 48).map{it.toDouble()}

        val data = xList.zip(yList).map{ a -> ScatterPoint(x = a.first, y=a.second, d = null, n=null, ac=null)}
        val regression = computeRegression.computeRegression(data)

        assert(roundNumber(regression?.slope ?: Double.NaN) == roundNumber(0.67461))
        assert(roundNumber(regression?.intercept ?: Double.NaN) == roundNumber(-38.45509))
        assert(roundNumber(regression?.rSquare ?: Double.NaN) == roundNumber(0.9548))
    }

    private fun roundNumber(n: Double): BigDecimal {
        return BigDecimal(n).setScale(ROUNDING_PRECISION, RoundingMode.HALF_UP)
    }

}
