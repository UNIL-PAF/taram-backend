package ch.unil.pafanalysis.analysis.steps.limma

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.canvas.draw.DottedLine
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.DottedBorder
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.Property
import com.itextpdf.layout.renderer.CellRenderer
import org.springframework.stereotype.Service


@Service
class LimmaPdf() : PdfCommon() {

    val cellFontSize = 8f

    val multiTestCorrText = mapOf(
        "BH" to "Benjamini-Hochberg (FDR)",
        "none" to "None"
    )

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, Limma::class.java)
        val parsedParams = gson.fromJson(step.parameters, LimmaParams::class.java)

        val stepDiv = Div()
        val description = "Limma is a statistical framework that applies linear models and empirical Bayes shrinkage to high‑throughput expression or abundance data."
        stepDiv.add(titleDiv("$stepNr. Limma", plotWidth = plotWidth, description = description, table = "Table $stepNr", nrProteins = step.nrProteinGroups, link = "$stepNr-${step.type}"))

        // 1. parameters
        val paramsData: List<Pair<String, String>> = listOf(
            Pair("Significance threshold:", parsedParams.signThres.toString()),
            Pair("Multiple testing correction:", multiTestCorrText[parsedParams.multiTestCorr] ?: "")
        )

        val paramsDiv = Div()
        paramsDiv.add(getTwoRowTable(paramsData))
        if(parsedParams.filterOnValid == true){
            paramsDiv.add(getOneRowTable(listOf(getParagraph("Only compute comparisons when there are at least ${parsedParams.minNrValid} valid (non-imputed) values in one group.", dense = true, bold = true))))
        }

        stepDiv.add(paramsDiv)
        stepDiv.add(createResTable(res, parsedParams.filterOnValid).setWidth(plotWidth -5f).setMarginTop(10f))
        return stepDiv
    }

    private fun createResTable(res: Limma, filterOnValid: Boolean?): Table {
        val table = Table(if(filterOnValid == true) 3 else 2)

        val header1 = Cell().setBorder(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
        header1.add(getParagraph("Comparison", bold = true, dense = true).setFontSize(cellFontSize))
        table.addCell(header1)

        val header2 = Cell().setBorder(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
        header2.add(getParagraph("Significant", bold = true, dense = true).setFontSize(cellFontSize))
        table.addCell(header2)

        if(filterOnValid == true){
            val header3 = Cell().setBorder(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
            header3.add(getParagraph("Filter passed", bold = true, dense = true).setFontSize(cellFontSize))
            table.addCell(header3)
        }

        res.comparisions?.forEach{ comp ->
            val cell1 = Cell().setBorder(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
            cell1.add(getParagraph("${comp.firstGroup} - ${comp.secondGroup}", dense = true).setFontSize(cellFontSize))
            table.addCell(cell1)

            val cell2 = Cell().setBorder(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
            cell2.add(getParagraph(comp.numberOfSignificant.toString(), dense = true).setFontSize(cellFontSize))
            table.addCell(cell2)

            if(filterOnValid == true){
                val cell3 = Cell().setBorder(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
                cell3.add(getParagraph(comp.nrPassedFilter.toString(), dense = true).setFontSize(cellFontSize))
                table.addCell(cell3)
            }
        }

        return table

    }

}
