package ch.unil.pafanalysis.analysis.steps.one_d_enrichment

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.StepNames
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import org.springframework.stereotype.Service
import java.text.DecimalFormat


@Service
class OneDEnrichmentPdf() : PdfCommon() {

    val cellFontSize = 8f

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, OneDEnrichment::class.java)
        val parsedParams = gson.fromJson(step.parameters, OneDEnrichmentParams::class.java)

        val stepDiv = Div()
        stepDiv.add(titleDiv("$stepNr - ${StepNames.getName(step?.type)}", plotWidth = plotWidth))

        val colTable = Table(9)
        colTable.setWidth(plotWidth)

        addHeaders(colTable)
        res.selResults?.forEach { row -> addRow(colTable, row) }

        stepDiv.add(colTable)
        return stepDiv
    }

    private fun addHeaders(colTable: Table){
        val headers = listOf("Column", "Type", "Name", "Size", "Score", "P-value", "Q-value", "Mean", "Median")
        headers.forEach{ h ->
            val headerPar = getParagraph(h, bold = true).setFontSize(cellFontSize)
            val rowNameCell = Cell().add(headerPar)
            rowNameCell.setBorder(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
            colTable.addCell(rowNameCell)
        }

    }

    private fun addRow(colTable: Table, row: EnrichmentRow) {
        addStringCell(colTable, row.column ?: "")
        addStringCell(colTable, row.type ?: "")
        addStringCell(colTable, row.name ?: "")
        addStringCell(colTable, row.size?.toString() ?: "")
        addDoubleCell(colTable, row.score)
        addDoubleCell(colTable, row.pvalue)
        addDoubleCell(colTable, row.qvalue)
        addDoubleCell(colTable, row.mean)
        addDoubleCell(colTable, row.median)
    }

    private fun addStringCell(colTable: Table, cellString: String) {
        val cell = Cell().add(getParagraph(cellString).setFontSize(cellFontSize))
        cell.setBorder(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
        colTable.addCell(cell)
    }

    private fun addDoubleCell(colTable: Table, cellVal: Double?) {
        val n = if (cellVal == 0.0) "0" else DecimalFormat("00.##E0").format(cellVal)
        addStringCell(colTable, n)
    }

}
