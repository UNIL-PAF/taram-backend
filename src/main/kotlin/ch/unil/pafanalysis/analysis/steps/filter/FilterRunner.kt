package ch.unil.pafanalysis.analysis.steps.filter

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
class FilterRunner() : CommonStep(), CommonRunner {

    @Autowired
    var asyncRunner: AsyncFilterRunner? = null

    override var type: AnalysisStepType? = AnalysisStepType.FILTER

    val defaultParams =
        FilterParams(removeReverse = true, removePotentialContaminant = true, removeOnlyIdentifiedBySite = true)

    override fun createPdf(step: AnalysisStep, document: Document?, pdf: PdfDocument): Document? {
        val title = Paragraph().add(Text(step.type).setBold())
        //val params = gson.fromJson(step.parameters, FilterParams::class.java)
        document?.add(title)
        if (step.comments !== null) document?.add(Paragraph().add(Text(step.comments)))
        return document
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val paramsString: String = params ?: ((step?.parameters) ?: gson.toJson(defaultParams))
        val newStep = runCommonStep(AnalysisStepType.FILTER, oldStepId, true, step, paramsString)

        asyncRunner?.runAsync(oldStepId, newStep, paramsString)
        return newStep!!
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = gson.fromJson(step.parameters, FilterParams::class.java)
        val origParams = if (origStep?.parameters != null) gson.fromJson(
            origStep.parameters,
            FilterParams::class.java
        ) else null

        return "Parameter(s) changed:"
            .plus(if (params.removeOnlyIdentifiedBySite != origParams?.removeOnlyIdentifiedBySite) " [Remove only identified by site: ${params.removeOnlyIdentifiedBySite}]" else "")
            .plus(if (params.removePotentialContaminant != origParams?.removePotentialContaminant) " [Remove potential contaminants: ${params.removePotentialContaminant}]" else "")
            .plus(if (params.removeReverse != origParams?.removeReverse) " [Remove reverse: ${params.removeReverse}]" else "")
    }

}