package ch.unil.pafanalysis.analysis.steps.umap

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.t_test.TTestParams
import ch.unil.pafanalysis.common.EchartsServer
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class UmapPdf() : PdfCommon() {

    @Autowired
    private var echartsServer: EchartsServer? = null

    fun createPdf(step: AnalysisStep, pdf: PdfDocument, plotWidth: Float, stepNr: Int): Div {
        val parsedParams = gson.fromJson(step.parameters, UmapParams::class.java)
        parsedParams.nrOfNeighbors

        val div = Div()
        div.add(titleDiv("$stepNr. UMAP", plotWidth))

        val description = "Examines similarities between samples and groups, needs imputed values."
        div.add(descriptionDiv(description))

        val colTable = Table(3)
        colTable.setWidth(plotWidth)
        val cellFifth = plotWidth/5

        // 1. parameters
        val paramsData: List<Pair<String, String>> = listOf(
            Pair("Number of neighbors:", parsedParams.nrOfNeighbors.toString()),
            Pair("Minimum distance:", parsedParams.minDistance.toString())
        )
        val paramsDiv = Div()
        paramsDiv.add(getTwoRowTable(paramsData))
        val leftCell = getParamsCell(paramsDiv, 2 * cellFifth, rightBorder = false)
        colTable.addCell(leftCell)
        div.add(colTable)

        div.add(Paragraph(" "))
        val plot = echartsServer?.makeEchartsPlot(step, pdf, plotWidth)
        div.add(plot)
        return div
    }

}
