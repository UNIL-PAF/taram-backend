package ch.unil.pafanalysis.analysis.steps.order_columns

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
class OrderColumnsRunner() : CommonStep(), CommonRunner {

    @Lazy
    @Autowired
    var asyncOrderColumnsRunner: AsyncOrderColumnsRunner? = null

    @Autowired
    var orderColumnsPdf: OrderColumnsPdf? = null

    override var type: AnalysisStepType? = AnalysisStepType.REMOVE_COLUMNS

    fun getParameters(step: AnalysisStep?): OrderColumnsParams {
        return if (step?.parameters != null) gson.fromJson(
            step?.parameters,
            OrderColumnsParams().javaClass
        ) else OrderColumnsParams()
    }

    override fun createPdf(step: AnalysisStep, pdf: PdfDocument, plotWidth: Float, stepNr: Int): Div? {
        return orderColumnsPdf?.createPdf(step, pdf, plotWidth, stepNr)
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val newStep = runCommonStep(type!!, oldStepId, true, step, params)
        asyncOrderColumnsRunner?.runAsync(oldStepId, newStep)
        return newStep!!
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = getParameters(step)
        val origParams = getParameters(origStep)

        return "Parameter(s) changed:"
            .plus(
                if (params.move?.size != origParams?.move?.size) " [Number of moves: ${params.move?.size}]" else ""
            ).plus(
                if (params.moveSelIntFirst != origParams?.moveSelIntFirst) " [Move selected intesity first: ${params.moveSelIntFirst}]" else ""
            )
    }

}