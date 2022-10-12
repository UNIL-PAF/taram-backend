package ch.unil.pafanalysis.analysis.steps.group_filter

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
class GroupFilterRunner() : CommonStep(), CommonRunner {

    @Autowired
    var asyncGroupFilterRunner: AsyncGroupFilterRunner? = null

    override var type: AnalysisStepType? = AnalysisStepType.GROUP_FILTER

    fun getParameters(step: AnalysisStep?): GroupFilterParams {
        return if(step?.parameters != null) gson.fromJson(step?.parameters, GroupFilterParams().javaClass) else GroupFilterParams()
    }

    override fun createPdf(step: AnalysisStep, document: Document?, pdf: PdfDocument): Document? {
        val title = Paragraph().add(Text(step.type).setBold())
        //val params = gson.fromJson(step.parameters, FilterParams::class.java)
        document?.add(title)
        if (step.comments !== null) document?.add(Paragraph().add(Text(step.comments)))
        return document
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val newStep = runCommonStep(type!!, oldStepId, true, step, params)
            asyncGroupFilterRunner?.runAsync(oldStepId, newStep)
        return newStep!!
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = gson.fromJson(step.parameters, GroupFilterParams::class.java)
        val origParams = if (origStep?.parameters != null) gson.fromJson(
            origStep.parameters,
            GroupFilterParams::class.java
        ) else null

        val filterInGroupText = when(params.filterInGroup ) {
            FilterInGroup.ONE_GROUP.value -> FilterInGroup.ONE_GROUP.text
            FilterInGroup.ALL_GROUPS.value -> FilterInGroup.ALL_GROUPS.text
            else -> "NONE"
        }

        return "Parameter(s) changed:"
            .plus(if (params.minNrValid != origParams?.minNrValid) " [Minimal number of valid: ${params.minNrValid}]" else "")
            .plus(if (params.filterInGroup != origParams?.filterInGroup) " [Number of valid entries required in: ${filterInGroupText}]" else "")
    }

}