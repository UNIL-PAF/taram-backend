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

        val description = "“Nr of valid ” = # quantified proteins. NaN are missing values."
        stepDiv.add(titleDiv("$stepNr. ${StepNames.getName(step?.type)}", plotWidth = plotWidth, description = description, link = "$stepNr-${step.type}"))

        val nrEntries = res.min?.size

        if(nrEntries != null){
            //val totChar = res.expNames?.joinToString("")?.length ?: 0
            val maxChar = 110

            var charsUsed = 0
            var start = 0
            var end = 0
            var tables = emptyList<Table>()
            var i = 0

            while(i < nrEntries){
                val longestChar = listOf(res.expNames?.get(i)?.length ?: 0, res.groups?.get(i)?.length ?: 0, 8).maxOrNull() ?: 8
                charsUsed += longestChar
                if(charsUsed > maxChar){
                    tables += createTable(res, start, end, plotWidth)
                    start = i
                    charsUsed = longestChar
                }
                i++
                end = i
            }

            // add last table if necessary
            if(end > start){
                tables += createTable(res, start, end, plotWidth)
            }

            tables.forEach{
                stepDiv.add(it)
                stepDiv.add(Paragraph(""))
            }

        }
        return stepDiv
    }

    private fun createTable(res: SummaryStat, start: Int, end: Int, plotWidth: Float): Table {
        val groups = res.groups ?: res.expNames?.map{""}

        val table = Table(end-start + 1) //.setWidth(plotWidth)
        addStringRow("Name", res.expNames, start, end, table, bold = true)
        addStringRow("Group", groups, start, end, table)
        addDoubleRow("Min", res.min, start, end, table)
        addDoubleRow("Max", res.max, start, end, table)
        addDoubleRow("Mean", res.mean, start, end, table)
        addDoubleRow("Median", res.median, start, end, table)
        addDoubleRow("Sum", res.sum, start, end, table)
        /*addDoubleRow("Std dev", res.stdDev, start, end, table)
        addDoubleRow("Std err", res.stdErr, start, end, table)
        addDoubleRow("Coef of var", res.coefOfVar, start, end, table)*/
        addIntRow("Nr of valid", res.nrValid, start, end, table)
        addIntRow("Nr of NaN", res.nrNaN, start, end, table)
        table.isKeepTogether = true
        return table
    }

    private fun addFirstCol(rowName: String, colTable: Table){
        val rowNamePar = getParagraph(rowName, bold = true, dense = true).setFontSize(cellFontSize)
        val rowNameCell = Cell().add(rowNamePar)
        rowNameCell.setBorder(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
        colTable.addCell(rowNameCell)
    }

    private fun addStringCell(cellString: String, colTable: Table, bold: Boolean = false){
        val p = getParagraph(cellString, bold = bold, dense = true).setFontSize(cellFontSize)
        val cell = Cell().add(p)
        cell.setBorder(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
        colTable.addCell(cell)
    }

    private fun addStringRow(rowName: String, data: List<String>?, start: Int, end: Int, colTable: Table, bold: Boolean = false){
       addFirstCol(rowName, colTable)

        data?.subList(start, end)?.forEach{ a ->
            addStringCell(a, colTable, bold)
        }
    }

    private fun addIntRow(rowName: String, data: List<Int>?, start: Int, end: Int, colTable: Table){
        addFirstCol(rowName, colTable)

        data?.subList(start, end)?.forEach{ a ->
            addStringCell(a.toString(), colTable)
        }
    }

    private fun addDoubleRow(rowName: String, data: List<Double>?, start: Int, end: Int, colTable: Table){
        addFirstCol(rowName, colTable)

        data?.subList(start, end)?.forEach{ a ->
            val n = when {
                a == 0.0 -> "0"
                abs(a) > 1e4 || abs(a) < 1e-2 -> DecimalFormat("#.###E0").format(a)
                else -> DecimalFormat("#.###").format(a)
            }
            addStringCell(n, colTable)
        }
    }
}
