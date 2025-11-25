package ch.unil.pafanalysis.analysis.steps.summary_stat

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.StepNames
import ch.unil.pafanalysis.pdf.PdfCommon
import ch.unil.pafanalysis.results.model.ResultType
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
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

        val nrPepDescription = if(res.nrOfPeps != null){
            "\n\"Nr of peptides\" = sum of " + if(step.analysis?.result?.type == ResultType.Spectronaut.value) {
                "\"NrOfPrecursorsIdentified\""
            } else "\"Razor.unique.peptides\""
        } else ""


        val description = "\"Nr of valid\" = number of quantified proteins. \n\"NaN\" (Not a Number) = missing values.$nrPepDescription"
        stepDiv.add(titleDiv("$stepNr. ${StepNames.getName(step.type)}", plotWidth = plotWidth, description = description, link = "$stepNr-${step.type}"))

        val table = Table(7 + (if(res.nrOfPeps != null) 1 else 0))
        addHeader(table, res)

        val groupSizes = res.groups?.groupingBy { it }?.eachCount()
        val selGroups = emptySet<String>().toMutableSet()

        res.expNames?.mapIndexed { i, name ->
            addStringCell(name, table)
            val groupName = res.groups?.get(i) ?: ""
            if(groupSizes == null){
                addStringCell("", table)
            } else if(groupSizes.contains(groupName) && !selGroups.contains(groupName)){
                addStringCell(groupName, table, bold = true, rowSpan = groupSizes[groupName])
                selGroups += groupName
            }
            addDoubleCell(res.min?.get(i), table)
            addDoubleCell(res.max?.get(i), table)
            addDoubleCell(res.median?.get(i), table)
            addStringCell(res.nrValid?.get(i).toString(), table, color = red)
            addStringCell(res.nrNaN?.get(i).toString(), table)
            if(res.nrOfPeps != null) addStringCell(res.nrOfPeps[i].toString(), table)
        }

        stepDiv.add(table)
        return stepDiv
    }

    private fun addHeader(table: Table, res: SummaryStat) {
        addStringCell("Name", table, bold = true)
        addStringCell("Group", table, bold = true)
        addStringCell("Min", table, bold = true)
        addStringCell("Max", table, bold = true)
        addStringCell("Median", table, bold = true)
        addStringCell("Nr of valid", table, bold = true, color = red)
        addStringCell("Nr of NaN", table, bold = true)
        if(res.nrOfPeps != null) addStringCell("Nr of peptides", table, bold = true)
    }

    private fun addStringCell(cellString: String, colTable: Table, bold: Boolean = false, rowSpan: Int? = null, color: DeviceRgb? = null) {
        val p = getParagraph(cellString, bold = bold, dense = true).setFontSize(cellFontSize)
        if(color != null) p.setFontColor(color)
        val cell = (if(rowSpan != null) Cell(rowSpan, 1) else Cell()).add(p)
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
