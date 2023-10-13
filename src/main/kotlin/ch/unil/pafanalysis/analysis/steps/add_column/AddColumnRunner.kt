package ch.unil.pafanalysis.analysis.steps.add_column

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class AddColumnRunner() : CommonStep(), CommonRunner {

    val version = "1.0"

    @Lazy
    @Autowired
    var asyncRunner: AsyncAddColumnRunner? = null

    @Autowired
    var addColumnPdf: AddColumnPdf? = null

    override var type: AnalysisStepType? = AnalysisStepType.ADD_COLUMN

    fun getParameters(step: AnalysisStep?): AddColumnParams {
        return if (step?.parameters != null) gson.fromJson(
            step?.parameters,
            AddColumnParams().javaClass
        ) else AddColumnParams()
    }

    override fun createPdf(step: AnalysisStep, pdf: PdfDocument, plotWidth: Float, stepNr: Int): Div? {
        return addColumnPdf?.createPdf(step, pdf, plotWidth, stepNr)
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val newStep = runCommonStep(type!!, version, oldStepId, true, step, params)
        asyncRunner?.runAsync(oldStepId, newStep)
        return newStep!!
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = getParameters(step)
        val origParams = getParameters(origStep)

        return "Parameter(s) changed:"
            .plus(
                if (params.selectedColumn != origParams?.selectedColumn) " [Selected column: ${params.selectedColumn}]" else ""
            )
    }

}