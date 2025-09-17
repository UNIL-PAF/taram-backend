package ch.unil.pafanalysis.analysis.steps.correlation_table

import ch.unil.pafanalysis.analysis.model.ExpInfo
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.StepException
import com.github.rcaller.rstuff.RCaller
import com.github.rcaller.rstuff.RCallerOptions
import com.github.rcaller.rstuff.RCode
import com.google.common.math.Quantiles
import org.springframework.stereotype.Service

@Service
class CorrelationTableComputation() {

    fun runCorrelation(
        ints: List<List<Double>>?,
        selHeaders: List<Header>?,
        expDetails: Map<String, ExpInfo>?,
        params: CorrelationTableParams
    ): CorrelationTable {

        val corrMatrix: List<List<Double>>? = ints?.mapIndexed{ i, col1 ->
            ints.mapIndexed{ k, col2 ->
                if(k == i) 1.0
                else if(k < i) Double.NaN
                else computeCorrR(col1, col2, params.correlationType)
            }
        }

        return CorrelationTable(
            correlationMatrix = corrMatrix,
            experimentNames = selHeaders?.map{it.experiment?.name ?: ""},
            groupNames = selHeaders?.map{expDetails?.get(it.experiment?.name)?.group ?: ""}
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