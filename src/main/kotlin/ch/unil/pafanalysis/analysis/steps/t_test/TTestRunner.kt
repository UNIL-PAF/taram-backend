package ch.unil.pafanalysis.analysis.steps.t_test

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TTestRunner() : CommonStep(), CommonRunner {

    @Autowired
    var asyncRunner: AsyncTTestRunner? = null

    override var type: AnalysisStepType? = AnalysisStepType.T_TEST

    val defaultParams =
        TTestParams()

    override fun createPdf(step: AnalysisStep, document: Document?, pdf: PdfDocument): Document? {
        val title = Paragraph().add(Text(step.type).setBold())
        //val params = gson.fromJson(step.parameters, FilterParams::class.java)
        document?.add(title)
        if (step.comments !== null) document?.add(Paragraph().add(Text(step.comments)))
        return document
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val paramsString: String = params ?: ((step?.parameters) ?: gson.toJson(defaultParams))
        val newStep = runCommonStep(type!!, oldStepId, true, step, paramsString)

        asyncRunner?.runAsync(oldStepId, newStep, paramsString)
        return newStep!!
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = gson.fromJson(step.parameters, TTestParams::class.java)
        val origParams = if (origStep?.parameters != null) gson.fromJson(
            origStep.parameters,
            TTestParams::class.java
        ) else null

        return "Parameter(s) changed:"
            .plus(if (params.field != origParams?.field) " [Remove only identified by site: ${params.field}]" else "")
    }

}