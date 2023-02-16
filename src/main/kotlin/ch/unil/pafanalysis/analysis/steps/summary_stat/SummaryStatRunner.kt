package ch.unil.pafanalysis.analysis.steps.summary_stat

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class SummaryStatRunner() : CommonStep(), CommonRunner {

    @Lazy
    @Autowired
    var asyncRunner: AsyncSummaryStatRunner? = null

    override var type: AnalysisStepType? = AnalysisStepType.SUMMARY_STAT

    fun getParameters(step: AnalysisStep?): SummaryStatParams {
        return if (step?.parameters != null) gson.fromJson(
            step?.parameters,
            SummaryStatParams().javaClass
        ) else SummaryStatParams()
    }

    override fun createPdf(step: AnalysisStep, document: Document?, pdf: PdfDocument): Document? {
        val title = Paragraph().add(Text(step.type).setBold())
        val transParams = gson.fromJson(step.parameters, SummaryStatParams::class.java)
        val selCol = Paragraph().add(Text("Selected column: ${transParams.intCol}"))
        document?.add(title)
        document?.add(selCol)
        if (step.comments !== null) document?.add(Paragraph().add(Text(step.comments)))
        return document
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val newStep = runCommonStep(type!!, oldStepId, false, step, params)
        asyncRunner?.runAsync(oldStepId, newStep)
        return newStep!!
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = getParameters(step)
        val origParams = getParameters(origStep)

        return "Parameter(s) changed:"
            .plus(if (params.intCol != origParams?.intCol) " [Column: ${params.intCol}]" else "")
    }

}