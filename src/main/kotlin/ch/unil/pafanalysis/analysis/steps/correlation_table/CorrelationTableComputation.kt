package ch.unil.pafanalysis.analysis.steps.correlation_table

import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.StepException
import com.github.rcaller.rstuff.RCaller
import com.github.rcaller.rstuff.RCallerOptions
import com.github.rcaller.rstuff.RCode
import org.springframework.stereotype.Service

@Service
class CorrelationTableComputation() {

    fun runCorrelation(
        ints: List<List<Double>>?,
        selHeaders: List<Header>?,
        params: CorrelationTableParams
    ): CorrelationTable {
        if(ints == null || selHeaders == null) throw StepException("No intensities or headers found.")

        val corrMatrix: List<OneCorrelation> = ints.flatMapIndexed { i, col1 ->
            ints.mapIndexed{ k, col2 ->
                if(k == i) OneCorrelation(i, k, 1.0)
                else if(k < i) null
                else OneCorrelation(k, i, computeCorrR(col1, col2, params.correlationType))
            }
        }.filterNotNull()

        return CorrelationTable(
            correlationMatrix = corrMatrix,
            experimentNames = selHeaders.map{it.experiment?.name ?: ""},
            )
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