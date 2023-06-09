package ch.unil.pafanalysis.analysis.steps.t_test

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TTestRunner() : CommonStep(), CommonRunner {

    @Autowired
    var asyncTTestRunner: AsyncTTestRunner? = null

    @Autowired
    var tTestPdf: TTestPdf? = null

    override var type: AnalysisStepType? = AnalysisStepType.T_TEST

    fun getParameters(step: AnalysisStep?): TTestParams {
        return if(step?.parameters != null) gson.fromJson(step?.parameters, TTestParams().javaClass) else TTestParams()
    }

    override fun createPdf(step: AnalysisStep, pdf: PdfDocument, plotWidth: Float, stepNr: Int): Div? {
        return tTestPdf?.createPdf(step, pdf, plotWidth, stepNr)
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val newStep = runCommonStep(type!!, oldStepId, true, step, params)
           asyncTTestRunner?.runAsync(oldStepId, newStep)
        return newStep!!
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = getParameters(step)
        val origParams = getParameters(origStep)

        return "Parameter(s) changed:"
            .plus(if (params.field != origParams?.field) " [Use intensity column: ${params.field}]" else "")
            .plus(if (params.signThres != origParams?.signThres) " [Significance threshold: ${params.field}]" else "")
            .plus(if (params.multiTestCorr != origParams?.multiTestCorr) " [Multiple testing correction: ${params.multiTestCorr}]" else "")
    }

}