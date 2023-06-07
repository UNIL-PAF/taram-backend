package ch.unil.pafanalysis.analysis.steps.normalization

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class NormalizationRunner() : CommonStep(), CommonRunner {

    @Lazy
    @Autowired
    var asyncRunner: AsyncNormalizationRunner? = null

    override var type: AnalysisStepType? = AnalysisStepType.NORMALIZATION

    fun getParameters(step: AnalysisStep?): NormalizationParams {
        return if (step?.parameters != null) gson.fromJson(
            step?.parameters,
            NormalizationParams().javaClass
        ) else NormalizationParams()
    }

    override fun createPdf(step: AnalysisStep, pdf: PdfDocument, plotWidth: Float, stepNr: Int): Div? {
        val title = Paragraph().add(Text(step.type).setBold())
        val transParams = gson.fromJson(step.parameters, NormalizationParams::class.java)
        val selCol = Paragraph().add(Text("Selected column: ${transParams.intCol}"))
        val div = Div()
        div.add(title)
        div.add(selCol)
        if (step.comments !== null) div.add(Paragraph().add(Text(step.comments)))
        return div
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val newStep = runCommonStep(type!!, oldStepId, true, step, params)
        asyncRunner?.runAsync(oldStepId, newStep)
        return newStep!!
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = getParameters(step)
        val origParams = getParameters(origStep)

        return "Parameter(s) changed:"
            .plus(if (params.intCol != origParams?.intCol) " [Column: ${params.intCol}]" else "")
            .plus(if (params.normalizationType != origParams?.normalizationType) " [Normalization: ${params.normalizationType}]" else "")
    }

}