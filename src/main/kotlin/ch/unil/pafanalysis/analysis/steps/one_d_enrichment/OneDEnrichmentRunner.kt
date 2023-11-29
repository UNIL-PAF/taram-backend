package ch.unil.pafanalysis.analysis.steps.one_d_enrichment

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
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

@Service
class OneDEnrichmentRunner() : CommonStep(), CommonRunner {

    val version = "1.0"

    @Lazy
    @Autowired
    var asyncRunner: AsyncOneDEnrichmentRunner? = null

    @Autowired
    var enrichmentPdf: OneDEnrichmentPdf? = null

    val enrichmentTableReader = EnrichmentTableReader()

    override var type: AnalysisStepType? = AnalysisStepType.ONE_D_ENRICHMENT

    @Autowired
    private var env: Environment? = null

    private fun getOutputPath(): String? {
        return env?.getProperty("output.path")
    }

    fun getParameters(step: AnalysisStep?): OneDEnrichmentParams {
        return if (step?.parameters != null) gson.fromJson(
            step?.parameters,
            OneDEnrichmentParams().javaClass
        ) else OneDEnrichmentParams()
    }

    override fun createPdf(step: AnalysisStep, pdf: PdfDocument, pageWidth: Float, stepNr: Int): Div? {
        return enrichmentPdf?.createPdf(step, pdf, pageWidth, stepNr)
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
    }

    fun getFullEnrichmentTable(step: AnalysisStep?): FullEnrichmentTable? {
        val result: OneDEnrichment = gson.fromJson(step?.results, OneDEnrichment().javaClass)
        val enrichmentResFilePath = getOutputPath() + step?.resultPath + "/" + result.enrichmentTable
        return enrichmentTableReader.readTable(enrichmentResFilePath)
    }

}