package ch.unil.pafanalysis.analysis.steps.correlation_table

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import org.springframework.stereotype.Service


@Service
class CorrelationTablePdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, CorrelationTable::class.java)
        val parsedParams = gson.fromJson(step.parameters, CorrelationTableParams::class.java)

        val stepDiv = Div()
        val description1 = "Assumption: the majority of the observed proteins remain unchanged."
        stepDiv.add(titleDiv("$stepNr. Correlation", plotWidth = plotWidth, description = description1, table = "Table $stepNr", nrProteins = step.nrProteinGroups, link = "$stepNr-${step.type}"))

        val colTable = Table(2)
        colTable.setWidth(plotWidth)
        val colWidth = plotWidth/12

        // 1. parameters
        val paramsDiv = Div().setPaddingLeft(2f)
        paramsDiv.add(getParagraph(parsedParams.correlationType.toString(), dense = true))
        colTable.addCell(getParamsCell(paramsDiv, 10*colWidth))

        // 2. data
        val middleDiv = Div()
        val tableData: List<Pair<String, String>> = listOf(
            "Min:" to String.format("%.2f", 0.6),
            "Max:" to String.format("%.2f", 0.9),
        )
        middleDiv.add(getTwoRowTable(tableData))
        colTable.addCell(getDataCell(middleDiv, 2 * colWidth))

        stepDiv.add(colTable)
        return stepDiv
    }

}
