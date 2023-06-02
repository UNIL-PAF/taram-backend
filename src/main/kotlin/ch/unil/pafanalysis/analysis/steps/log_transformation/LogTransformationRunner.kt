package ch.unil.pafanalysis.analysis.steps.log_transformation

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class LogTransformationRunner() : CommonStep(), CommonRunner {

    @Lazy
    @Autowired
    var asyncTransformationRunner: AsyncLogTransformationRunner? = null

    override var type: AnalysisStepType? = AnalysisStepType.LOG_TRANSFORMATION

    fun getParameters(step: AnalysisStep?): LogTransformationParams {
        return if (step?.parameters != null) gson.fromJson(
            step?.parameters,
            LogTransformationParams().javaClass
        ) else LogTransformationParams()
    }

    override fun createPdf(step: AnalysisStep, document: Document?, pdf: PdfDocument, pageSize: PageSize?, stepNr: Int): Document? {
        val title = Paragraph().add(Text(step.type).setBold())
        val transParams = gson.fromJson(step.parameters, LogTransformationParams::class.java)
        val selCol = Paragraph().add(Text("Selected column: ${transParams.intCol}"))
        document?.add(title)
        document?.add(selCol)
        if (step.comments !== null) document?.add(Paragraph().add(Text(step.comments)))
        return document
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val newStep = runCommonStep(type!!, oldStepId, true, step, params)
        asyncTransformationRunner?.runAsync(oldStepId, newStep)
        return newStep!!
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = getParameters(step)
        val origParams = getParameters(origStep)

        return "Parameter(s) changed:"
            .plus(if (params.intCol != origParams?.intCol) " [Column: ${params.intCol}]" else "")
            .plus(if (params.transformationType != origParams?.transformationType) " [Transformation: ${params.transformationType}]" else "")
    }

}