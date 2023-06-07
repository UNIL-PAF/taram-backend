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
        document?.add(titleDiv("$stepNr - Boxplot", step?.nrProteinGroups, null, plotWidth))
        document?.add(Paragraph(" "))
        val plot = echartsServer?.makeEchartsPlot(step, pdf, pageSize, document)
        document?.add(plot)
        if (step.comments !== null) document?.add(Paragraph().add(Text(step.comments)))

        return document
    }

}
