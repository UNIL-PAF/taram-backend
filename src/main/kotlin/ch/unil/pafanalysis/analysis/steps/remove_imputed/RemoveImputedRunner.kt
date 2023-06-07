package ch.unil.pafanalysis.analysis.steps.remove_imputed

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
class RemoveImputedRunner() : CommonStep(), CommonRunner {

    @Lazy
    @Autowired
    var asyncRemoveImputedRunner: AsyncRemoveImputedRunner? = null

    override var type: AnalysisStepType? = AnalysisStepType.REMOVE_IMPUTED

    fun getParameters(step: AnalysisStep?): RemoveImputedParams {
        return if (step?.parameters != null) gson.fromJson(
            step?.parameters,
            RemoveImputedParams().javaClass
        ) else RemoveImputedParams()
    }

    override fun createPdf(step: AnalysisStep, pdf: PdfDocument, plotWidth: Float, stepNr: Int): Div {
        val title = Paragraph().add(Text(step.type).setBold())
        val transParams = gson.fromJson(step.parameters, RemoveImputedParams::class.java)
        val selCol = Paragraph().add(Text("Selected column: ${transParams.replaceBy}"))
        val div = Div()
        div.add(title)
        div.add(selCol)
        if (step.comments !== null) div.add(Paragraph().add(Text(step.comments)))
        return div
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val newStep = runCommonStep(type!!, oldStepId, true, step, params)
        asyncRemoveImputedRunner?.runAsync(oldStepId, newStep)
        return newStep!!
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = getParameters(step)
        val origParams = getParameters(origStep)

        return "Parameter(s) changed:"
            .plus(if (params.replaceBy != origParams?.replaceBy) " [Replace by: ${params.replaceBy}]" else "")
    }

}