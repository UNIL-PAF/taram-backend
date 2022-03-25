package ch.unil.pafanalysis.controllers

import ch.unil.pafanalysis.analysis.model.AnalysisStepParams
import ch.unil.pafanalysis.analysis.model.AnalysisStepType.*
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.analysis.steps.boxplot.BoxPlotRunner
import ch.unil.pafanalysis.analysis.steps.quality_control.QualityControlRunner
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@CrossOrigin(origins = ["http://localhost:3000"], maxAge = 3600)
@RestController
// This means that this class is a Controller
@RequestMapping(path = ["/analysis-step"])
class AnalysisStepController {

    @Autowired
    private var qualityControlRunner: QualityControlRunner? = null

    @Autowired
    private var boxPlotRunner: BoxPlotRunner? = null

    @PostMapping(path = ["/add-to/{stepId}"])
    @ResponseBody
    fun addTo(@RequestBody stepParams: AnalysisStepParams, @PathVariable(value = "stepId") stepId: Int): String? {
        val status: String? = when (stepParams.type) {
            QUALITY_CONTROL.value -> qualityControlRunner?.run(stepId)
            BOXPLOT.value -> boxPlotRunner?.run(stepId)
            else -> throw StepException("Analysis step [" + stepParams.type + "] not found.")
        }
        return status
    }
}