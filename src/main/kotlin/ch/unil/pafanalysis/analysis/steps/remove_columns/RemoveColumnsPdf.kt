package ch.unil.pafanalysis.analysis.steps.remove_columns

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import org.springframework.stereotype.Service


@Service
class RemoveColumnsPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, RemoveColumns::class.java)

        val stepDiv = Div()
        stepDiv.add(titleDiv("$stepNr - Remove columns", plotWidth = plotWidth))

        val colTable = Table(3)
        colTable.setWidth(plotWidth)
        val cellFifth = plotWidth/5

        // 1. parameters
        val paramsDiv = Div()
        paramsDiv.add(getTwoRowTable(listOf(Pair("Nr of columns to remove:", res.nrOfColumnsRemoved.toString()))))
        colTable.addCell(getParamsCell(paramsDiv, 2*cellFifth))

        // 2. data
        val tableData: List<Pair<String, String>> = listOf(
            Pair("Nr of columns after removal:", res.nrOfColumns.toString()),
        )
        val middleDiv = Div()
        middleDiv.add(getTwoRowTable(tableData))
        colTable.addCell(getDataCell(middleDiv, 2 * cellFifth))

        // 3. results
        val rightDiv = Div()
        rightDiv.add(getParagraph("${step.nrProteinGroups} protein groups"))
        rightDiv.add(getParagraph("Table ${step.tableNr}", bold = true, underline = true))
        colTable.addCell(getResultCell(rightDiv, cellFifth))

        stepDiv.add(colTable)
        return stepDiv
    }

}
