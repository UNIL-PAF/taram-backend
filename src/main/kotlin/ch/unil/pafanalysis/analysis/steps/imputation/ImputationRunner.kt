package ch.unil.pafanalysis.analysis.steps.imputation

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
class ImputationRunner() : CommonStep(), CommonRunner {

    @Lazy
    @Autowired
    var asyncTransformationRunner: AsyncImputationRunner? = null

    override var type: AnalysisStepType? = AnalysisStepType.IMPUTATION

    fun getParameters(step: AnalysisStep?): ImputationParams {
        return if (step?.parameters != null) gson.fromJson(
            step?.parameters,
            ImputationParams().javaClass
        ) else ImputationParams()
    }

    override fun createPdf(step: AnalysisStep, document: Document?, pdf: PdfDocument): Document? {
        val title = Paragraph().add(Text(step.type).setBold())
        val params = gson.fromJson(step.parameters, ImputationParams::class.java)
        val selCol = Paragraph().add(Text("Selected column: ${params.intCol}"))
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

        fun normImpDiff(): String {
            var text = ""
            if (params.normImputationParams?.width != origParams.normImputationParams?.width) {
                text += "[Width:  ${params.normImputationParams?.width}]"
            }
            if (params.normImputationParams?.seed != origParams.normImputationParams?.seed) {
                text += "[Seed:  ${params.normImputationParams?.seed}]"
            }
            if (params.normImputationParams?.downshift != origParams.normImputationParams?.downshift) {
                text += "[Downshift:  ${params.normImputationParams?.downshift}]"
            }
            return text
        }

        return "Parameter(s) changed:"
            .plus(if (params.intCol != origParams?.intCol) " [Column: ${params.intCol}]" else "")
            .plus(if (params.imputationType != origParams?.imputationType) " [Imputation: ${params.imputationType}]" else "")
            .plus(if (params.imputationType == ImputationType.NORMAL.value && origParams?.imputationType == ImputationType.NORMAL.value) normImpDiff() else "")
    }

}