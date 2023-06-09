package ch.unil.pafanalysis.analysis.steps.t_test

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
class TTestPdf() : PdfCommon() {

    val multiTestCorrText = mapOf(
        "BH" to "Benjamini & Hochberg (FDR)",
        "none" to "None"
    )

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, TTest::class.java)
        val parsedParams = gson.fromJson(step.parameters, TTestParams::class.java)

        val stepDiv = Div()
        stepDiv.add(titleDiv("$stepNr - t-test", step.nrProteinGroups, step.tableNr, plotWidth = plotWidth))

        val colTable = Table(2)
        colTable.setWidth(plotWidth)

        val pLeft = Paragraph()
        pLeft.add("Number of significant results:").setFontSize(fontSizeConst)

        val tableData: List<Pair<String, Paragraph?>> = res.comparisions?.map{ comp ->
            Pair("${comp.firstGroup} - ${comp.secondGroup}", Paragraph(comp.numberOfSignificant.toString()))
        } ?: emptyList()

        val leftDiv = Div()
        leftDiv.add(pLeft)
        leftDiv.add(addTwoRowTable(tableData))

        val leftCell = Cell().add(leftDiv)
        leftCell.setWidth(plotWidth/2)
        leftCell.setBorder(Border.NO_BORDER)
        colTable.addCell(leftCell)

        val paramsList = listOf<Paragraph>(
            Paragraph("Significance threshold: ${parsedParams.signThres}"),
            Paragraph("Multiple testing correction: ${multiTestCorrText[parsedParams.multiTestCorr]}")
        )

        val rightCell = Cell().add(parametersDiv(paramsList))
        colTable.addCell(rightCell)
        stepDiv.add(colTable)

        return stepDiv
    }

}
