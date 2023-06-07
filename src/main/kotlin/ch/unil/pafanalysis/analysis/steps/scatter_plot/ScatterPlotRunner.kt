package ch.unil.pafanalysis.analysis.steps.scatter_plot

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.EchartsPlot
import ch.unil.pafanalysis.common.EchartsServer
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class ScatterPlotRunner() : CommonStep(), CommonRunner {

    override var type: AnalysisStepType? = AnalysisStepType.SCATTER_PLOT

    @Autowired
    private var echartsServer: EchartsServer? = null

    @Autowired
    var asyncRunner: AsyncScatterPlotRunner? = null

    fun getParameters(step: AnalysisStep?): ScatterPlotParams {
        return if(step?.parameters != null) gson.fromJson(step?.parameters, ScatterPlotParams().javaClass) else ScatterPlotParams()
    }

    override fun createPdf(step: AnalysisStep, pdf: PdfDocument, plotWidth: Float, stepNr: Int): Div {
        val title = Paragraph().add(Text(step.type).setBold())
        val params = gson.fromJson(step.parameters, ScatterPlotParams::class.java)
        val selCol = Paragraph().add(Text("Selected x-axis: ${params?.xAxis}"))

        val div = Div()
        div.add(title)
        div.add(selCol)
        val plot = echartsServer?.makeEchartsPlot(step, pdf, plotWidth)
        div.add(plot)

        if (step.comments !== null) div.add(Paragraph().add(Text(step.comments)))

        return div
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val newStep = runCommonStep(type!!, oldStepId, false, step, params)
        asyncRunner?.runAsync(oldStepId, newStep)
        return newStep!!
    }

    override fun updatePlotOptions(step: AnalysisStep, echartsPlot: EchartsPlot): String {
        val newResults = gson.fromJson(step.results, ScatterPlot().javaClass).copy(plot = echartsPlot)
        val newStep = step.copy(results = gson.toJson(newResults))
        analysisStepRepository?.saveAndFlush(newStep)
        return echartsPlot.echartsHash.toString()
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = getParameters(step)
        val origParams = getParameters(origStep)

        // there might be differences in selected proteins, which we ignore
        val message = (if (params.xAxis != origParams?.xAxis) " [x-axis: ${params.xAxis}]" else "") +
                (if (params.yAxis != origParams?.yAxis) " [y-axis: ${params.yAxis}]" else "")

        return if(message != "") "Parameter(s) changed:".plus(message) else null
    }

}