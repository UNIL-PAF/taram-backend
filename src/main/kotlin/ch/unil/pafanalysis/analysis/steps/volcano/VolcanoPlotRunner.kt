package ch.unil.pafanalysis.analysis.steps.volcano

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.EchartsPlot
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class VolcanoPlotRunner() : CommonStep(), CommonRunner {

    override var type: AnalysisStepType? = AnalysisStepType.VOLCANO_PLOT

    @Autowired
    var asyncRunner: AsyncVolcanoPlotRunner? = null

    private fun getParams(step: AnalysisStep?): VolcanoPlotParams {
        val params = if(step?.parameters != null) gson.fromJson(step?.parameters, VolcanoPlotParams().javaClass) else null
        val pValThresh = params?.pValThresh
        return VolcanoPlotParams(pValThresh = pValThresh)
    }

    override fun createPdf(step: AnalysisStep, document: Document?, pdf: PdfDocument): Document? {
        val title = Paragraph().add(Text(step.type).setBold())
        val params = gson.fromJson(step.parameters, VolcanoPlotParams::class.java)
        val selCol = Paragraph().add(Text("P-val threshold column: ${params?.pValThresh}"))

        document?.add(title)
        document?.add(selCol)
        document?.add(makeEchartsPlot(step, pdf))
        if (step.comments !== null) document?.add(Paragraph().add(Text(step.comments)))

        return document
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val newStep = runCommonStep(type!!, oldStepId, false, step, params)
        val parsedParams: VolcanoPlotParams? = gson.fromJson(params, VolcanoPlotParams::class.java) ?: getParams(newStep)

        val paramsHash = hashComp.computeStringHash(parsedParams?.toString())
        val stepWithHash = newStep?.copy(parametersHash = paramsHash, parameters = gson.toJson(parsedParams))
        val stepWithDiff = stepWithHash?.copy(copyDifference = getCopyDifference(stepWithHash))

        asyncRunner?.runAsync(oldStepId, stepWithDiff, params)
        return stepWithDiff!!
    }

    override fun updatePlotOptions(step: AnalysisStep, echartsPlot: EchartsPlot): String {
        val newResults = gson.fromJson(step.results, VolcanoPlot().javaClass).copy(plot = echartsPlot)
        val newStep = step.copy(results = gson.toJson(newResults))
        analysisStepRepository?.saveAndFlush(newStep)
        return echartsPlot.echartsHash.toString()
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = gson.fromJson(step.parameters, VolcanoPlotParams().javaClass)
        val origParams =
            if (origStep?.parameters != null) gson.fromJson(origStep?.parameters, VolcanoPlotParams().javaClass) else null

        return "Parameter(s) changed:"
            .plus(if (params.pValThresh != origParams?.pValThresh) " [P-value threshold: ${params.pValThresh}]" else "")
    }

}