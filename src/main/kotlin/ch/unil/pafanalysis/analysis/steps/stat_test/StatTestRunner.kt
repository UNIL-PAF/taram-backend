package ch.unil.pafanalysis.analysis.steps.stat_test

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.limma.AsyncLimmaRunner
import ch.unil.pafanalysis.analysis.steps.limma.LimmaParams
import ch.unil.pafanalysis.analysis.steps.limma.LimmaPdf
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StatTestRunner() : CommonStep(), CommonRunner {

    val version = "1.0"

    @Autowired
    var asyncLimmaRunner: AsyncLimmaRunner? = null

    @Autowired
    var limmaPdf: LimmaPdf? = null

    override var type: AnalysisStepType? = AnalysisStepType.LIMMA

    fun getParameters(step: AnalysisStep?): LimmaParams {
        return if(step?.parameters != null) gson.fromJson(step.parameters, LimmaParams().javaClass) else LimmaParams()
    }

    override fun createPdf(step: AnalysisStep, pdf: PdfDocument, plotWidth: Float, stepNr: Int): Div? {
        return limmaPdf?.createPdf(step, pdf, plotWidth, stepNr)
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val newStep = runCommonStep(type!!, version, oldStepId, true, step, params)
           asyncLimmaRunner?.runAsync(oldStepId, newStep)
        return newStep!!
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = getParameters(step)
        val origParams = getParameters(origStep)

        return "Parameter(s) changed:"
            .plus(if (params.field != origParams.field) " [Use intensity column: ${params.field}]" else "")
            .plus(if (params.signThres != origParams.signThres) " [Significance threshold: ${params.field}]" else "")
    }

}