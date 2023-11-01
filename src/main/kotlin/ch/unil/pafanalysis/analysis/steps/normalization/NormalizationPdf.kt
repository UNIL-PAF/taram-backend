package ch.unil.pafanalysis.analysis.steps.normalization

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
class NormalizationPdf() : PdfCommon() {

    val normType = mapOf(
        "median" to "Median",
        "mean" to "Mean",
        "none" to "None"
    )

    fun normText(params: NormalizationParams): String {
        val myType = normType[params.normalizationType]
        return if(params.normalizationCalculation == NormalizationCalculation.DIVISION.value){
            "Divide by $myType"
        }else{
            "Substract $myType"
        }
    }

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, Normalization::class.java)
        val parsedParams = gson.fromJson(step.parameters, NormalizationParams::class.java)

        val stepDiv = Div()
        stepDiv.add(titleDiv("$stepNr - Normalization", plotWidth = plotWidth))

        val colTable = Table(3)
        colTable.setWidth(plotWidth)
        val cellFifth = plotWidth/5

        // 1. parameters
        val paramsDiv = Div()
        paramsDiv.add(getParagraph(normText(parsedParams)))
        colTable.addCell(getParamsCell(paramsDiv, 2*cellFifth))

        // 2. data
        val middleDiv = Div()
        val tableData: List<Pair<String, String>> = listOf(
            "Min:" to String.format("%.2f", res.min),
            "Max:" to String.format("%.2f", res.max),
        )
        middleDiv.add(getTwoRowTable(tableData))
        colTable.addCell(getDataCell(middleDiv, 2 * cellFifth))

        // 3. results
        val rightDiv = Div()
        rightDiv.add(getParagraph("${step.nrProteinGroups} protein groups"))
        rightDiv.add(getParagraph("Table $stepNr", bold = true, underline = true))
        colTable.addCell(getResultCell(rightDiv, cellFifth))

        stepDiv.add(colTable)
        return stepDiv
    }

}
