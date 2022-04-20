package ch.unil.pafanalysis.analysis.steps.boxplot

import ch.unil.pafanalysis.analysis.steps.StepException
import com.github.rcaller.rstuff.RCaller
import com.github.rcaller.rstuff.RCallerOptions
import com.github.rcaller.rstuff.RCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStream


@Service
class BoxPlotR {
    @Value(value = "classpath:r_scripts/boxplot/boxplot.R")
    private val rSource: Resource? = null

    fun runR(data: DoubleArray): DoubleArray {
        val inputStream: InputStream? = rSource?.inputStream
        val content: String = inputStream?.bufferedReader()?.use(BufferedReader::readText)
            ?: throw StepException("Could not read R-script.")

        val code = RCode.create()
        code.addRCode(content)
        code.addDoubleArray("data", data)
        code.addRCode("result <- run(data)")

        val caller = RCaller.create(code, RCallerOptions.create())
        caller.runAndReturnResult("result")

        return caller.parser.getAsDoubleArray("result")
    }

}
