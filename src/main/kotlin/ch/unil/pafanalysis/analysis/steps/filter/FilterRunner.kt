package ch.unil.pafanalysis.analysis.steps.filter

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
import org.springframework.stereotype.Service
import kotlin.math.max

@Service
class FilterRunner() : CommonStep(), CommonRunner {

    val version = "1.0"

    @Autowired
    var asyncFilterRunner: AsyncFilterRunner? = null

    @Autowired
    var filterPdf: FilterPdf? = null

    override var type: AnalysisStepType? = AnalysisStepType.FILTER

    fun getParameters(step: AnalysisStep?): FilterParams {
        return if (step?.parameters != null) gson.fromJson(
            step?.parameters,
            FilterParams().javaClass
        ) else FilterParams()
    }

    override fun createPdf(step: AnalysisStep, pdf: PdfDocument, pageWidth: Float, stepNr: Int): Div? {
        return filterPdf?.createPdf(step, pdf, pageWidth, stepNr)
    }

    override fun run(oldStepId: Int, step: AnalysisStep?, params: String?): AnalysisStep {
        val newStep = runCommonStep(type!!, version, oldStepId, true, step, params)
        asyncFilterRunner?.runAsync(oldStepId, newStep)
        return newStep!!
    }

    fun colFilterToString(colFilter: ColFilter): String {
        return if(colFilter.removeSelected) "remove " else "keep "
            .plus(colFilter.colName)
            .plus(" ${colFilter.comparator.symbol} ")
            .plus(colFilter.compareToValue)
    }

    override fun getCopyDifference(step: AnalysisStep, origStep: AnalysisStep?): String? {
        val params = getParameters(step)
        val origParams = getParameters(origStep)

        val customs = params.colFilters?.map{ colFilterToString(it) }?.toSet() ?: emptySet()
        val origCustoms = origParams.colFilters?.map{ colFilterToString(it) }?.toSet() ?: emptySet()

        val customMessages = customs.subtract(customs.intersect(origCustoms)).map{ c -> " [${c}]"}.joinToString()
        val customRemoved = if(origCustoms.size > customs.size) " [Custom filter(s) removed.]" else ""

        return "Parameter(s) changed:"
            .plus(customRemoved)
            .plus(if (params.removeOnlyIdentifiedBySite != origParams?.removeOnlyIdentifiedBySite) " [Remove only identified by site: ${params.removeOnlyIdentifiedBySite}]" else "")
            .plus(if (params.removePotentialContaminant != origParams?.removePotentialContaminant) " [Remove potential contaminants: ${params.removePotentialContaminant}]" else "")
            .plus(if (params.removeReverse != origParams?.removeReverse) " [Remove reverse: ${params.removeReverse}]" else "")
            .plus(customMessages)
    }

}