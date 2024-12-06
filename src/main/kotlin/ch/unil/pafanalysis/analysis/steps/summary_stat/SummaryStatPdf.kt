package ch.unil.pafanalysis.analysis.steps.summary_stat

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.StepNames
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import org.springframework.stereotype.Service
import java.text.DecimalFormat
import kotlin.math.abs


@Service
class SummaryStatPdf() : PdfCommon() {

    val cellFontSize = 8f

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, SummaryStat::class.java)

        val stepDiv = Div()

        val description = "\"Nr of valid\" = Number of quantified proteins. \n\"NaN\" (Not a Number) = missing values."
        stepDiv.add(titleDiv("$stepNr. ${StepNames.getName(step?.type)}", plotWidth = plotWidth, description = description, link = "$stepNr-${step.type}"))

        val table = Table(7)
        addHeader(table)
        res.expNames?.mapIndexed { i, name ->
            addStringCell(name, table)
            addStringCell(res.groups?.get(i) ?: "", table)
            addDoubleCell(res.min?.get(i), table)
            addDoubleCell(res.max?.get(i), table)
            addDoubleCell(res.median?.get(i), table)
            addStringCell(res.nrValid?.get(i).toString(), table)
            addStringCell(res.nrNaN?.get(i).toString(), table)
        }

        stepDiv.add(table)
        return stepDiv
    }

    private fun addHeader(table: Table) {
        addStringCell("Name", table, bold = true)
        addStringCell("Group", table, bold = true)
        addStringCell("Min", table, bold = true)
        addStringCell("Max", table, bold = true)
        addStringCell("Median", table, bold = true)
        addStringCell("Nr of valid", table, bold = true)
        addStringCell("Nr of NaN", table, bold = true)
    }

    private fun addStringCell(cellString: String, colTable: Table, bold: Boolean = false){
        val p = getParagraph(cellString, bold = bold, dense = true).setFontSize(cellFontSize)
        val cell = Cell().add(p)
        cell.setBorder(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
        colTable.addCell(cell)
    }

    private fun addDoubleCell(data: Double?, table: Table){
        val n = when {
            data == null -> "NaN"
            data == 0.0 -> "0"
            abs(data) > 1e4 || abs(data) < 1e-2 -> DecimalFormat("#.##E0").format(data)
            else -> DecimalFormat("#.##").format(data)
        }
        addStringCell(n, table)
    }

}
