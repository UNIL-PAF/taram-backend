package ch.unil.pafanalysis.analysis.steps.quality_control

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.service.AnalysisStepService
import ch.unil.pafanalysis.analysis.steps.StepException
import com.github.rcaller.rstuff.RCaller
import com.github.rcaller.rstuff.RCallerOptions
import com.github.rcaller.rstuff.RCode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStream
import kotlin.concurrent.thread


@Service
class QualityControlR {
    @Autowired
    private var analysisStepService: AnalysisStepService? = null

    @Autowired
    private var env: Environment? = null

    @Value(value = "classpath:r_scripts/quality_control/quality_control.R")
    private val rSource: Resource? = null

    fun runR(analysisStep: AnalysisStep) {
        thread(start = true, isDaemon = true) {
            val inputStream: InputStream? = rSource?.inputStream
            val content: String = inputStream?.bufferedReader()?.use(BufferedReader::readText)
                ?: throw StepException("Could not read R-script.")

            val code = RCode.create()
            code.addRCode(content)

            val outputRoot: String? = env?.getProperty("output.path.maxquant")
            code.addString("output_path", outputRoot!!+"/"+analysisStep.resultPath)
            code.addRCode("result <- run(output_path)")

            val caller = RCaller.create(code, RCallerOptions.create())
            caller.runAndReturnResult("result")

            println(caller.parser.getAsStringArray("result")[0])
            // lets sleep for 30 seconds
            //Thread.sleep(3_000)

            // update analysisStep status to Done
            analysisStepService?.setAnalysisStepStatus(analysisStep.id!!, AnalysisStepStatus.DONE)
        }

    }

}

/*

        val step: AnalysisStep = try {
            val initialResult = createInitialResult(maxQuantPath)

            AnalysisStep(
                resultTablePath = newTable.name,
                status = AnalysisStepStatus.DONE.value,
                type = type,
                analysis = analysis,
                lastModifDate = lastModif,
                results = gson.toJson(initialResult)
            )
        } catch (e: StepException) {
            AnalysisStep(
                status = AnalysisStepStatus.ERROR.value,
                type = type,
                error = e.message,
                analysis = analysis,
                lastModifDate = lastModif
            )
        }
*/