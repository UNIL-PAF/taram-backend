package ch.unil.pafanalysis.controllers

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.model.AnalysisStepType.INITIAL_RESULT
import ch.unil.pafanalysis.analysis.model.AnalysisStepType.VOLCANO_PLOT
import ch.unil.pafanalysis.analysis.model.AnalysisStepType.SCATTER_PLOT
import ch.unil.pafanalysis.analysis.model.AnalysisStepType.PCA
import ch.unil.pafanalysis.analysis.service.*
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.EchartsPlot
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.analysis.steps.initial_result.InitialResultRunner
import ch.unil.pafanalysis.analysis.steps.pca.PcaRunner
import ch.unil.pafanalysis.analysis.steps.scatter_plot.ScatterPlotRunner
import ch.unil.pafanalysis.analysis.steps.volcano.VolcanoPlotRunner
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

@CrossOrigin(origins = ["http://localhost:3000", "http://taram-dev.dcsr.unil.ch", "http://taram.dcsr.unil.ch"], maxAge = 3600)
@RestController
// This means that this class is a Controller
@RequestMapping(path = ["/analysis-step"])
class AnalysisStepController {

    @Autowired
    private var analysisStepRepository: AnalysisStepRepository? = null

    @Autowired
    private var analysisStepService: AnalysisStepService? = null

    @Autowired
    private var asyncAnaysisStepService: AsyncAnalysisStepService? = null

    @Autowired
    private var initialResult: InitialResultRunner? = null

    @Autowired
    private var commonStep: CommonStep? = null

    @Autowired
    private var volcanoPlotRunner: VolcanoPlotRunner? = null

    @Autowired
    private var scatterPlotRunner: ScatterPlotRunner? = null

    @Autowired
    private var pcaRunner: PcaRunner? = null

    @Autowired
    private var proteinTableService: ProteinTableService? = null

    @Autowired
    private var fullProtTableService: FullProteinTableService? = null

    @GetMapping(path = ["/protein-table/{stepId}"])
    fun getProteinTable(@PathVariable(value = "stepId") stepId: Int): ProteinTable? {
        val step = analysisStepRepository?.findById(stepId)
        return proteinTableService?.getProteinTable(step, commonStep?.getSelProts(step), step?.analysis?.result?.type)
    }

    @GetMapping(path = ["/full-protein-table/{stepId}"])
    fun getFullProteinTable(@PathVariable(value = "stepId") stepId: Int): FullProteinTable? {
        val step = analysisStepRepository?.findById(stepId)
        return fullProtTableService?.getTable(step)
    }

    @PostMapping(path = ["/add-to/{stepId}"])
    @ResponseBody
    fun addTo(@RequestBody stepParams: AnalysisStepParams, @PathVariable(value = "stepId") stepId: Int): String? {
        val step = analysisStepRepository?.findById(stepId)
        asyncAnaysisStepService?.setAllStepsStatus(step, AnalysisStepStatus.IDLE)
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

    @PostMapping(path = ["/switch-sel/{selId}/step-id/{stepId}"])
    @ResponseBody
    fun switchSel(
        @PathVariable(value = "selId") selId: String,
        @PathVariable(value = "stepId") stepId: Int
    ): String? {
        val step = analysisStepRepository?.findById(stepId)
        val selProts = when (step?.type) {
            VOLCANO_PLOT.value -> volcanoPlotRunner?.switchSelProt(step, selId)
            SCATTER_PLOT.value -> scatterPlotRunner?.switchSelProt(step, selId)
            PCA.value -> pcaRunner?.switchSelExp(step, selId)
            else -> throw StepException("Cannot select items for [$step?.type].")
        }
        return selProts?.joinToString(", ")
    }

    @PostMapping(path = ["/parameters/{stepId}"])
    @ResponseBody
    fun parameters(
        @RequestBody stepParams: String,
        @PathVariable(value = "stepId") stepId: Int,
        @RequestParam doNotRun: Boolean? = null,
    ): String? {
        val analysisStep = analysisStepRepository?.findById(stepId)

        if(doNotRun == true){
            // we don't have to recompute the parameter hash in this case
            analysisStepRepository?.saveAndFlush(analysisStep?.copy(parameters = stepParams)!!)
        }else{
            asyncAnaysisStepService?.setAllStepsStatus(analysisStep, AnalysisStepStatus.IDLE)

            try {
                if (analysisStep?.type == INITIAL_RESULT.value) {
                    initialResult?.updateColumnParams(analysisStep, stepParams)
                } else {
                    commonStep?.getRunner(analysisStep?.type)?.run(
                        analysisStep?.beforeId!!,
                        analysisStep,
                        stepParams
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val errorStep = analysisStep?.copy(status = AnalysisStepStatus.ERROR.value, error = e.message)
                analysisStepRepository?.saveAndFlush(errorStep!!)
                errorStep
            }
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

    @GetMapping(path = ["/table/{stepId}"])
    fun getTable(@PathVariable(value = "stepId") stepId: Int): ResponseEntity<ByteArray>? {
        val tableFile = analysisStepService?.getTable(stepId)
        val inputStream: InputStream = FileInputStream(tableFile)
        val contents = inputStream.readAllBytes()

        val headers = HttpHeaders();
        headers.contentType = MediaType.TEXT_PLAIN;
        val filename = "output.txt";
        headers.setContentDispositionFormData(filename, filename);
        headers.cacheControl = "must-revalidate, post-check=0, pre-check=0";
        val response = ResponseEntity(contents, headers, HttpStatus.OK);
        return response;
    }

    @GetMapping(path = ["/result/{stepId}"])
    fun getResult(@PathVariable(value = "stepId") stepId: Int): ResponseEntity<ByteArray>? {
        val step = analysisStepRepository?.findById(stepId)
        val contents = commonStep?.getRunner(step?.type)?.getResultByteArray(step)

        val headers = HttpHeaders();
        headers.contentType = MediaType.TEXT_PLAIN;
        val filename = "output.txt";
        headers.setContentDispositionFormData(filename, filename);
        headers.cacheControl = "must-revalidate, post-check=0, pre-check=0";
        val response = ResponseEntity(contents, headers, HttpStatus.OK);
        return response;
    }

    @GetMapping(path = ["/zip/{stepId}"])
    fun getZip(
        @PathVariable(value = "stepId") stepId: Int,
        @RequestParam svg: Boolean? = null,
        @RequestParam png: Boolean? = null,
    ): ResponseEntity<ByteArray>? {
        var response: ResponseEntity<ByteArray>? = null
        try {
            val zipFile: String? = analysisStepService?.getZip(stepId, svg, png)

            val inputStream: InputStream = FileInputStream(zipFile)
            val contents = inputStream.readAllBytes()
            inputStream.close()
            File(zipFile).delete()

            val headers = HttpHeaders();
            headers.contentType = MediaType.MULTIPART_MIXED;
            val filename = "output.zip";
            headers.setContentDispositionFormData(filename, filename);
            headers.cacheControl = "must-revalidate, post-check=0, pre-check=0";
            response = ResponseEntity(contents, headers, HttpStatus.OK);
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return response;
    }

}