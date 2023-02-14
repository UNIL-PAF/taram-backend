package ch.unil.pafanalysis.analysis.steps.volcano

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.EchartsPlot
import ch.unil.pafanalysis.common.EchartsServer
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
    var asyncVolcanoPlotRunner: AsyncVolcanoPlotRunner? = null

    @Autowired
    private var echartsServer: EchartsServer? = null

    fun getParameters(step: AnalysisStep?): VolcanoPlotParams {
        return if(step?.parameters != null) gson.fromJson(step?.parameters, VolcanoPlotParams().javaClass) else VolcanoPlotParams()
    }

    override fun createPdf(step: AnalysisStep, document: Document?, pdf: PdfDocument): Document? {
        val title = Paragraph().add(Text(step.type).setBold())
        val params = gson.fromJson(step.parameters, VolcanoPlotParams::class.java)
        val selCol = Paragraph().add(Text("P-val threshold column: ${params?.pValThresh}"))

        document?.add(title)
        document?.add(selCol)
        document?.add(echartsServer?.makeEchartsPlot(step, pdf))
        if (step.comments !== null) document?.add(Paragraph().add(Text(step.comments)))

        return document
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val newStep = runCommonStep(type!!, oldStepId, false, step, params)
        asyncVolcanoPlotRunner?.runAsync(oldStepId, newStep)
        return newStep!!
    }

    override fun updatePlotOptions(step: AnalysisStep, echartsPlot: EchartsPlot): String {
        val newResults = gson.fromJson(step.results, VolcanoPlot().javaClass).copy(plot = echartsPlot)
        val newStep = step.copy(results = gson.toJson(newResults))
        analysisStepRepository?.saveAndFlush(newStep)
        return echartsPlot.echartsHash.toString()
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = getParameters(step)
        val origParams = getParameters(origStep)

        // there might be differences in selected proteins, which we ignore
        val message = (if (params.pValThresh != origParams?.pValThresh) " [P-value threshold: ${params.pValThresh}]" else "")
            .plus(if (params.fcThresh != origParams?.fcThresh) " [Significance threshold: ${params.fcThresh}]" else "")
            .plus(if (params.useAdjustedPVal != origParams?.useAdjustedPVal) " [Use adjusted p-value: ${params.useAdjustedPVal}]" else "")
            .plus(if (params.log10PVal != origParams?.log10PVal) " [Use log10 p-value: ${params.log10PVal}]" else "")

        return if(message != "") "Parameter(s) changed:".plus(message) else null
    }

    fun switchSelProt(step: AnalysisStep?, proteinAc: String): List<String>? {
        val origParams = gson.fromJson(step?.parameters, VolcanoPlotParams().javaClass)
        val origList = origParams.selProteins ?: emptyList()
        val newList = if(origList.contains(proteinAc)) origList.filter{it != proteinAc} else origList.plus(proteinAc)
        val newParams = origParams.copy(selProteins = newList)
        analysisStepRepository?.saveAndFlush(step?.copy(parameters = gson.toJson(newParams))!!)
        return newList
    }

}