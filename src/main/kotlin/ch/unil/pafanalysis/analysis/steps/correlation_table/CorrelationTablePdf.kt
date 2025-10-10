package ch.unil.pafanalysis.analysis.steps.correlation_table

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.common.EchartsServer
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class CorrelationTablePdf() : PdfCommon() {

    @Autowired
    private var echartsServer: EchartsServer? = null

    fun createPdf(step: AnalysisStep, pdf: PdfDocument, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, CorrelationTable::class.java)
        val params = gson.fromJson(step.parameters, CorrelationTableParams::class.java)

        val div = Div()
        val description = if(params.correlationType == CorrelationType.PEARSON.value) "Pearson R² shows how closely the results of two proteomics experiments match. A value close to 1 indicates a strong linear relationship, while a value close to 0 suggests little to no linear relationship. It only captures linear patterns and does not account for non-linear relationships."
            else "Spearman R² shows how closely the results of two proteomics experiments match. A value close to 1 indicates a strong relationship, while a value close to 0 suggests suggests little to no consistent trend between the two. It can capture non-linear but monotonic patterns, meaning it reflects relationships where values consistently increase or decrease together."
        div.add(titleDiv("$stepNr. Correlations", plotWidth, description = description, link = "$stepNr-${step.type}"))

        div.add(Paragraph(" "))
        val plot = echartsServer?.makeEchartsPlot(step, pdf, plotWidth)
        div.add(plot)

        return div
    }

}
