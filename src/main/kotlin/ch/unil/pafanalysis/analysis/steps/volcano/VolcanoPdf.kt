package ch.unil.pafanalysis.analysis.steps.volcano

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.common.EchartsServer
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class VolcanoPdf() : PdfCommon() {

    @Autowired
    private var echartsServer: EchartsServer? = null

    fun createPdf(step: AnalysisStep, pdf: PdfDocument, plotWidth: Float, stepNr: Int): Div {
        val div = Div()
        val description = "y-axis: can be either p- or q-values (-log10)."
        div.add(titleDiv("$stepNr. Volcano plot", plotWidth, description))

        div.add(Paragraph(" "))
        val plot = echartsServer?.makeEchartsPlot(step, pdf, plotWidth)
        div.add(plot)
        return div
    }

}
