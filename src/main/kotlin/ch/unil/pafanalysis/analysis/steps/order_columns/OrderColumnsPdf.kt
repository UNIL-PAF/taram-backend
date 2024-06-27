package ch.unil.pafanalysis.analysis.steps.order_columns

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.t_test.TTestParams
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import org.springframework.stereotype.Service


@Service
class OrderColumnsPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, OrderColumns::class.java)
        val parsedParams = gson.fromJson(step.parameters, OrderColumnsParams::class.java)

        val stepDiv = Div()
        val description = "Table cleanup."
        stepDiv.add(titleDiv("$stepNr. Order columns", plotWidth = plotWidth, description))

        val colTable = Table(3)
        colTable.setWidth(plotWidth)
        val cellFifth = plotWidth/5

        // 1. parameters
        val paramsDiv = Div().setPaddingLeft(2f)
        if(parsedParams.moveSelIntFirst == true) paramsDiv.add(getParagraph("Move default intensity columns [${step.columnInfo?.columnMapping?.intCol}] first.", dense = true))
        colTable.addCell(getParamsCell(paramsDiv, 2*cellFifth))

        // 2. data
        colTable.addCell(getDataCell(Div(), 2 * cellFifth))

        // 3. results
        val rightDiv = Div()
        rightDiv.add(getParagraph("${step.nrProteinGroups} protein groups"))
        rightDiv.add(getTableParagraph("Table-$stepNr"))
        colTable.addCell(getResultCell(rightDiv, cellFifth))

        stepDiv.add(colTable)
        return stepDiv
    }

}
