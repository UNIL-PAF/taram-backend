package ch.unil.pafanalysis.analysis.steps.remove_columns

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import org.springframework.stereotype.Service


@Service
class RemoveColumnsPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        //val res = gson.fromJson(step.results, RemoveColumns::class.java)

        val stepDiv = Div()
        val description = "Columns not necessary for analysis are removed."
        stepDiv.add(titleDiv("$stepNr. Remove columns", plotWidth = plotWidth, description = description, table = "Table $stepNr", nrProteins = step.nrProteinGroups, link = "$stepNr-${step.type}"))

        /*
        val colTable = Table(2)
        colTable.setWidth(plotWidth)
        val colWidth = plotWidth/12

        // 1. parameters
        val paramsDiv = Div()
        paramsDiv.add(getTwoRowTable(listOf(Pair("Nr of columns to remove:", res.nrOfColumnsRemoved.toString()))))
        colTable.addCell(getParamsCell(paramsDiv, 8*colWidth))

        // 2. data
        val tableData: List<Pair<String, String>> = listOf(
            Pair("Nr of columns after removal:", res.nrOfColumns.toString()),
        )
        val middleDiv = Div()
        middleDiv.add(getTwoRowTable(tableData))
        colTable.addCell(getDataCell(middleDiv, 4 * colWidth))

        stepDiv.add(colTable)
         */
        return stepDiv
    }

}
