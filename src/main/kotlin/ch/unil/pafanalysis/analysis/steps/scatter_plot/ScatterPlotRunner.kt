package ch.unil.pafanalysis.analysis.steps.scatter_plot

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.EchartsPlot
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class ScatterPlotRunner() : CommonStep(), CommonRunner {

    val version = "1.0"

    override var type: AnalysisStepType? = AnalysisStepType.SCATTER_PLOT

    @Autowired
    var asyncRunner: AsyncScatterPlotRunner? = null

    @Autowired
    var scatterPlotPdf: ScatterPlotPdf? = null

    fun getParameters(step: AnalysisStep?): ScatterPlotParams {
        return if(step?.parameters != null) gson.fromJson(step?.parameters, ScatterPlotParams().javaClass) else ScatterPlotParams()
    }

    override fun createPdf(step: AnalysisStep, pdf: PdfDocument, plotWidth: Float, stepNr: Int): Div? {
       return scatterPlotPdf?.createPdf(step, pdf, plotWidth, stepNr)
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val newStep = runCommonStep(type!!, version, oldStepId, false, step, params)
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

    override fun switchSel(step: AnalysisStep?, proteinAc: String): List<String>? {
        val origParams = gson.fromJson(step?.parameters, ScatterPlotParams().javaClass)
        val origList = origParams.selProteins ?: emptyList()
        val newList = if(origList.contains(proteinAc)) origList.filter{it != proteinAc} else origList.plus(proteinAc)
        val newParams = origParams.copy(selProteins = newList)
        analysisStepRepository?.saveAndFlush(step?.copy(parameters = gson.toJson(newParams))!!)
        return newList
    }

}