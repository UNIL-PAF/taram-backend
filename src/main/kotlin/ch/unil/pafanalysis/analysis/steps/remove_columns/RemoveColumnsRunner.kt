package ch.unil.pafanalysis.analysis.steps.remove_columns

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
class RemoveColumnsRunner() : CommonStep(), CommonRunner {

    val version = "1.0"

    @Lazy
    @Autowired
    var asyncRemoveColumnsRunner: AsyncRemoveColumnsRunner? = null

    @Autowired
    var removeColumnsPdf: RemoveColumnsPdf? = null

    override var type: AnalysisStepType? = AnalysisStepType.REMOVE_COLUMNS

    fun getParameters(step: AnalysisStep?): RemoveColumnsParams {
        return if (step?.parameters != null) gson.fromJson(
            step?.parameters,
            RemoveColumnsParams().javaClass
        ) else RemoveColumnsParams()
    }

    override fun createPdf(step: AnalysisStep, pdf: PdfDocument, plotWidth: Float, stepNr: Int): Div? {
        return removeColumnsPdf?.createPdf(step, pdf, plotWidth, stepNr)
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val newStep = runCommonStep(type!!, version, oldStepId, true, step, params)
        asyncRemoveColumnsRunner?.runAsync(oldStepId, newStep)
        return newStep!!
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = getParameters(step)
        val origParams = getParameters(origStep)

        return "Parameter(s) changed:"
            .plus(
                if (params.keepIdxs?.joinToString(",") != origParams?.keepIdxs?.joinToString(",")) " [Replace by: ${
                    params.keepIdxs?.joinToString(
                        separator = ","
                    )
                }]" else ""
            )
    }

}