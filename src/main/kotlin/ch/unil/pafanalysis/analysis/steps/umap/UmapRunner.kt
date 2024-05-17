package ch.unil.pafanalysis.analysis.steps.umap

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.EchartsPlot
import ch.unil.pafanalysis.common.EchartsServer
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class UmapRunner() : CommonStep(), CommonRunner {

    val version = "1.0"

    override var type: AnalysisStepType? = AnalysisStepType.UMAP

    @Autowired
    var asyncBoxplotRunner: AsyncUmapRunner? = null

    @Autowired
    var umapPdf: UmapPdf? = null

    fun getParameters(step: AnalysisStep?): UmapParams {
        return if(step?.parameters != null) gson.fromJson(step?.parameters, UmapParams().javaClass) else UmapParams()
    }

    override fun createPdf(step: AnalysisStep, pdf: PdfDocument, plotWidth: Float, stepNr: Int): Div? {
        return umapPdf?.createPdf(step, pdf, plotWidth, stepNr)
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val newStep = runCommonStep(type!!, version, oldStepId, false, step, params)
        asyncBoxplotRunner?.runAsync(oldStepId, newStep)
        return newStep!!
    }

    override fun updatePlotOptions(step: AnalysisStep, echartsPlot: EchartsPlot): String {
        val newResults = gson.fromJson(step.results, UmapRes().javaClass).copy(plot = echartsPlot)
        val newStep = step.copy(results = gson.toJson(newResults))
        analysisStepRepository?.saveAndFlush(newStep)
        return echartsPlot.echartsHash.toString()
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = getParameters(step)
        val origParams = getParameters(origStep)

        // there might be differences in selected proteins, which we ignore
        val message = (if (params.column != origParams?.column) " [Column: ${params.column}]" else "")
            .plus(if (params.nrOfNeighbors != origParams?.nrOfNeighbors) " [Number of neighbors: ${params.nrOfNeighbors}]" else "")

        return if(message != "") "Parameter(s) changed:".plus(message) else null
    }

    override fun switchSel(step: AnalysisStep?, expName: String): List<String>? {
        val origParams = gson.fromJson(step?.parameters, UmapParams().javaClass)
        val origList = origParams.selExps ?: emptyList()
        val newList = if(origList.contains(expName)) origList.filter{it != expName} else origList.plus(expName)
        val newParams = origParams.copy(selExps = newList)
        analysisStepRepository?.saveAndFlush(step?.copy(parameters = gson.toJson(newParams))!!)
        return newList
    }

    override fun getResult(step: AnalysisStep?): String? {
        val res = gson.fromJson(step?.results, UmapRes().javaClass)
        val withoutPlot = res.copy(plot = null)
        return gson.toJson(withoutPlot)
    }

}