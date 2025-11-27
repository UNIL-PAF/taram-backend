package ch.unil.pafanalysis.analysis.steps.correlation_table

import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.StepException
import com.github.rcaller.rstuff.RCaller
import com.github.rcaller.rstuff.RCallerOptions
import com.github.rcaller.rstuff.RCode
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation
import org.springframework.stereotype.Service
import kotlin.math.pow

@Service
class CorrelationTableComputation() {

    val pearsons = PearsonsCorrelation()
    val spearman = SpearmansCorrelation()

    fun runCorrelation(
        ints: List<List<Double>>?,
        selHeaders: List<Header>?,
        params: CorrelationTableParams,
        withR: Boolean = true
    ): CorrelationTable {
        if(ints == null || selHeaders == null) throw StepException("No intensities or headers found.")

        val corrMatrix: List<OneCorrelation> = ints.flatMapIndexed { i, col1 ->
            ints.mapIndexed{ k, col2 ->
                if(k == i) OneCorrelation(i, k, 1.0)
                else if(k < i) null
                else OneCorrelation(k, i, computeCorr(col1, col2, params.correlationType, withR))
            }
        }.filterNotNull()

        return CorrelationTable(
            correlationMatrix = corrMatrix,
            experimentNames = selHeaders.map{it.experiment?.name ?: ""},
            )
    }

    private fun computeCorr(x: List<Double>, y: List<Double>, method: String?, withR: Boolean): Double {
        return if(withR) computeCorrR(x, y, method) else computeCorrJ(x, y, method)
    }

    private fun computeCorrJ(x: List<Double>, y: List<Double>, method: String?): Double {
        val (a, b) = x.zip(y).filter{ a -> ! (a.first.isNaN() || a.second.isNaN()) }.unzip()
        return if(method == "spearman"){
            spearman.correlation(a.toDoubleArray(), b.toDoubleArray())
        }else{
            pearsons.correlation(a.toDoubleArray(), b.toDoubleArray())
        }.pow(2.0)
    }


    private fun computeCorrR(x: List<Double>, y: List<Double>, method: String?): Double {
        val code = RCode.create()
        code.addDoubleArray("x", x.toDoubleArray())
        code.addDoubleArray("y", y.toDoubleArray())

        code.addString("method", method)
        code.addRCode("r_corr <- cor(x, y, method = method, use = \"complete.obs\")")
        code.addRCode("r2_corr <- r_corr^2")
        // Create caller
        val caller = RCaller.create(code, RCallerOptions.create())

        // Run and pull variable from R
        caller.runAndReturnResult("r2_corr")

        // Extract as double
        return caller.parser.getAsDoubleArray("r2_corr")[0]
    }

}