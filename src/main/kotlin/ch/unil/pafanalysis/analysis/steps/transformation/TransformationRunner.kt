package ch.unil.pafanalysis.analysis.steps.transformation

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.service.AnalysisStepRepository
import ch.unil.pafanalysis.analysis.steps.CommonResult
import ch.unil.pafanalysis.analysis.steps.CommonRunner
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.analysis.steps.t_test.TTestParams
import ch.unil.pafanalysis.common.Crc32HashComputations
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.WriteTableData
import com.google.common.math.Quantiles
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import kotlin.math.ln

@Service
class TransformationRunner() : CommonStep(), CommonRunner {

    @Autowired
    var asyncTransformationRunner: AsyncTransformationRunner? = null

    override var type: AnalysisStepType? = AnalysisStepType.TRANSFORMATION

    fun getParameters(step: AnalysisStep?): TransformationParams {
        return if(step?.parameters != null) gson.fromJson(step?.parameters, TransformationParams().javaClass) else TransformationParams()
    }

    override fun createPdf(step: AnalysisStep, document: Document?, pdf: PdfDocument): Document? {
        val title = Paragraph().add(Text(step.type).setBold())
        val transParams = gson.fromJson(step.parameters, TransformationParams::class.java)
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
        val params = gson.fromJson(step.parameters, TransformationParams::class.java)
        val origParams = if (origStep?.parameters != null) gson.fromJson(
            origStep.parameters,
            TransformationParams::class.java
        ) else null

        return "Parameter(s) changed:"
            .plus(if (params.intCol != origParams?.intCol) " [Column: ${params.intCol}]" else "")
            .plus(if (params.normalizationType != origParams?.normalizationType) " [Normalization: ${params.normalizationType}]" else "")
            .plus(if (params.transformationType != origParams?.transformationType) " [Transformation: ${params.transformationType}]" else "")
    }

}