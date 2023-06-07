package ch.unil.pafanalysis.analysis.steps.boxplot

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.common.EchartsServer
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.UnitValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class BoxPlotPdf() : PdfCommon() {

    @Autowired
    private var echartsServer: EchartsServer? = null

    fun createPdf(step: AnalysisStep, document: Document?, pdf: PdfDocument, pageSize: PageSize?, stepNr: Int): Document? {
        document?.add(horizontalLineDiv())
        val plotWidth = getPlotWidth(pageSize, document)
        val div = Div()
        div.add(titleDiv("$stepNr - Boxplot", step?.nrProteinGroups, null, plotWidth))
        div.add(Paragraph(" "))
        val plot = echartsServer?.makeEchartsPlot(step, pdf, pageSize, document)
        div.add(plot)
        if (step.comments !== null) div.add(Paragraph().add(Text(step.comments)))
        div.setKeepTogether(true)
        document?.add(div)

        return document
    }

}
