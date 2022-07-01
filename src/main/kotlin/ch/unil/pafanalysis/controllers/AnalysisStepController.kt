package ch.unil.pafanalysis.controllers

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepParams
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.model.AnalysisStepType.*
import ch.unil.pafanalysis.analysis.service.AnalysisStepRepository
import ch.unil.pafanalysis.analysis.service.AnalysisStepService
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.EchartsPlot
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.analysis.steps.boxplot.BoxPlotRunner
import ch.unil.pafanalysis.analysis.steps.initial_result.InitialResultRunner
import ch.unil.pafanalysis.analysis.steps.quality_control.QualityControlRunner
import ch.unil.pafanalysis.analysis.steps.transformation.TransformationRunner
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@CrossOrigin(origins = ["http://localhost:3000"], maxAge = 3600)
@RestController
// This means that this class is a Controller
@RequestMapping(path = ["/analysis-step"])
class AnalysisStepController {

    @Autowired
    private var analysisStepRepository: AnalysisStepRepository? = null

    @Autowired
    private var analysisStepService: AnalysisStepService? = null

    @Autowired
    private var qualityControlRunner: QualityControlRunner? = null

    @Autowired
    private var boxPlotRunner: BoxPlotRunner? = null

    @Autowired
    private var initialResult: InitialResultRunner? = null

    @Autowired
    private var commonStep: CommonStep? = null

    @Autowired
    private var transformationRunner: TransformationRunner? = null

    @PostMapping(path = ["/add-to/{stepId}"])
    @ResponseBody
    fun addTo(@RequestBody stepParams: AnalysisStepParams, @PathVariable(value = "stepId") stepId: Int): String? {
        return commonStep?.addStep(stepId, stepParams)?.status
    }

    @PostMapping(path = ["/plot-options/{stepId}"])
    @ResponseBody
    fun plotOptions(@RequestBody echartsPlot: EchartsPlot, @PathVariable(value = "stepId") stepId: Int): String? {
        analysisStepService?.updatePlotOptions(stepId, echartsPlot)
        return "done"
    }

    @DeleteMapping(path = ["/{stepId}"])
    @ResponseBody
    fun deleteStep(@PathVariable(value = "stepId") stepId: Int): Int? {
        return analysisStepService?.deleteStep(stepId)
    }

    @PostMapping(path = ["/parameters/{stepId}"])
    @ResponseBody
    fun parameters(@RequestBody stepParams: String, @PathVariable(value = "stepId") stepId: Int): String? {
        val analysisStep = analysisStepRepository?.findById(stepId)

        analysisStepService?.setAllStepsStatus(analysisStep, AnalysisStepStatus.IDLE)

        try {
            when (analysisStep?.type) {
                INITIAL_RESULT.value -> initialResult?.updateColumnParams(analysisStep, stepParams)
                BOXPLOT.value -> boxPlotRunner?.updateParams(analysisStep, stepParams)
                TRANSFORMATION.value -> transformationRunner?.updateParams(analysisStep, stepParams)
                else -> throw RuntimeException("Analysis step [" + analysisStep?.type + "] not found.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val errorStep = analysisStep?.copy(status = AnalysisStepStatus.ERROR.value, error = e.message)
            analysisStepRepository?.save(errorStep!!)
            errorStep
        }

        return analysisStep?.status
    }

    @PostMapping(path = ["/comment/{stepId}"])
    @ResponseBody
    fun updateComment(
        @RequestBody comment: String,
        @PathVariable(value = "stepId") stepId: Int
    ): Boolean? {
        return analysisStepService?.updateComment(stepId, comment)
    }

    @DeleteMapping(path = ["/comment/{stepId}"])
    @ResponseBody
    fun deleteComment(@PathVariable(value = "stepId") stepId: Int): Boolean? {
        return analysisStepService?.updateComment(stepId, null)
    }

}