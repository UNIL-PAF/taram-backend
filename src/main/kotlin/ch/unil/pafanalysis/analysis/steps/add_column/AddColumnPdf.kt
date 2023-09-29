package ch.unil.pafanalysis.analysis.steps.add_column

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Table
import org.springframework.stereotype.Service


@Service
class AddColumnPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val parsedParams = gson.fromJson(step.parameters, AddColumnParams::class.java)

        val stepDiv = Div()
        stepDiv.add(titleDiv("$stepNr - Add column", plotWidth = plotWidth))

        val colTable = Table(3)
        colTable.setWidth(plotWidth)
        val cellFifth = plotWidth/5

        // 1. parameters
        val paramsDiv = Div()
        if(parsedParams.addConditionNames == true) paramsDiv.add(getParagraph("Add conditions to table headers."))
        colTable.addCell(getParamsCell(paramsDiv, 2*cellFifth))

        // 2. data
        val middleDiv = Div()
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
