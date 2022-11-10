package ch.unil.pafanalysis.analysis.steps.boxplot

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.EchartsPlot
import ch.unil.pafanalysis.analysis.steps.filter.FilterParams
import ch.unil.pafanalysis.analysis.steps.transformation.ImputationType
import ch.unil.pafanalysis.analysis.steps.transformation.NormalizationType
import ch.unil.pafanalysis.analysis.steps.transformation.TransformationParams
import ch.unil.pafanalysis.analysis.steps.transformation.TransformationType
import ch.unil.pafanalysis.common.EchartsServer
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.swing.Box


@Service
class BoxPlotRunner() : CommonStep(), CommonRunner {

    override var type: AnalysisStepType? = AnalysisStepType.BOXPLOT

    @Autowired
    private var echartsServer: EchartsServer? = null

    @Autowired
    var asyncBoxplotRunner: AsyncBoxPlotRunner? = null

    fun getParameters(step: AnalysisStep?): BoxPlotParams {
        return if(step?.parameters != null) gson.fromJson(step?.parameters, BoxPlotParams().javaClass) else BoxPlotParams()
    }

    override fun createPdf(step: AnalysisStep, document: Document?, pdf: PdfDocument): Document? {
        val title = Paragraph().add(Text(step.type).setBold())
        val params = gson.fromJson(step.parameters, BoxPlotParams::class.java)
        val selCol = Paragraph().add(Text("Selected column: ${params?.column}"))

        document?.add(title)
        document?.add(selCol)
        document?.add(echartsServer?.makeEchartsPlot(step, pdf))
        if (step.comments !== null) document?.add(Paragraph().add(Text(step.comments)))

        return document
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val newStep = runCommonStep(type!!, oldStepId, false, step, params)
        asyncBoxplotRunner?.runAsync(oldStepId, newStep)
        return newStep!!
    }

    override fun updatePlotOptions(step: AnalysisStep, echartsPlot: EchartsPlot): String {
        val newResults = gson.fromJson(step.results, BoxPlot().javaClass).copy(plot = echartsPlot)
        val newStep = step.copy(results = gson.toJson(newResults))
        analysisStepRepository?.saveAndFlush(newStep)
        return echartsPlot.echartsHash.toString()
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = getParameters(step)
        val origParams = getParameters(origStep)

        // there might be differences in selected proteins, which we ignore
        val message = (if (params.column != origParams?.column) " [Column: ${params.column}]" else "")
            .plus(if (params.logScale != origParams?.logScale) " [Log scale: ${params.logScale}]" else "")

        return if(message != "") "Parameter(s) changed:".plus(message) else null
    }

}