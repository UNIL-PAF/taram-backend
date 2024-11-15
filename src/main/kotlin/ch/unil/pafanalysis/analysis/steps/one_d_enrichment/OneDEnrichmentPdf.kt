package ch.unil.pafanalysis.analysis.steps.one_d_enrichment

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.StepNames
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.io.font.otf.GlyphLine
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.layout.LayoutContext
import com.itextpdf.layout.layout.LayoutResult
import com.itextpdf.layout.renderer.CellRenderer
import com.itextpdf.layout.renderer.IRenderer
import com.itextpdf.layout.splitting.DefaultSplitCharacters
import org.springframework.stereotype.Service
import java.text.DecimalFormat


@Service
class OneDEnrichmentPdf() : PdfCommon() {

    val cellFontSize = 8f

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, OneDEnrichment::class.java)
        val parsedParams = gson.fromJson(step.parameters, OneDEnrichmentParams::class.java)

        val stepDiv = Div()
        val description = "Table is first ranked by (usually) fold change. Distribution of each annotation term is then evaluated to see if it is enriched toward the top or bottom. FDR-filtered table."
        val enrichmentTable = getTableParagraph("Enrichment table $stepNr").setWidth(85f)
        stepDiv.add(titleDiv("$stepNr. ${StepNames.getName(step?.type)}", plotWidth = plotWidth, description = description, table = "Table $stepNr", nrProteins = step.nrProteinGroups, extraParagraph = enrichmentTable, link = "$stepNr-${step.type}"))

        val colTable = Table(1)
        colTable.setWidth(plotWidth)

        // 1. parameters
        val paramsData: List<Pair<String, String>> = listOf(
            Pair("Annotation file:", res.annotation?.origFileName ?: ""),
            Pair("Annotation info:", getAnnotationString(res.annotation) ?: ""),
            Pair("Selected annotations:", res.annotation?.selHeaderNames?.joinToString(separator = ", ") ?: ""),
            Pair("Selected column(s):", res.selColumns?.joinToString("; ") ?: ""),
            Pair("Significance threshold:", parsedParams.threshold.toString() ?: ""),
            Pair("Multiple testing correction:", if(parsedParams.fdrCorrection == true) "Benjamini & Hochberg (FDR)" else "None"),
        )
        val paramsDiv = Div()
        paramsDiv.add(getTwoRowTable(paramsData, leftColMinWidth=110f))
        val leftCell = getParamsCell(paramsDiv, plotWidth)
        colTable.addCell(leftCell)
        stepDiv.add(colTable)

        // 3. Table of selected results
        val title = getParagraph("Selected results:", bold = true, underline = false)
        stepDiv.add(title)

        val selResTable = Table(7)
        selResTable.setWidth(plotWidth)
        selResTable.setKeepTogether(true)

        addHeaders(selResTable, parsedParams.fdrCorrection)
        res.selResults?.forEach { row -> addRow(selResTable, row, parsedParams.fdrCorrection) }

        stepDiv.add(selResTable)
        return stepDiv
    }

    private fun getAnnotationString(annotation: EnrichmentAnnotationInfo?): String? {
        return annotation?.name + ", " +
                (if(annotation?.description != null && annotation?.description != "undefined") annotation.description + ", " else "") +
                annotation?.creationString + ", " +
                annotation?.nrEntries + " entries."
    }

    private fun addHeaders(colTable: Table, multiTestCorr: Boolean?){
        val pVal = if(multiTestCorr == true) "Adj. p-value" else "P-value"
        val headers = listOf("Column", "Type", "Name", "Size", "Score", pVal, "Median")
        headers.forEach{ h ->
            val headerPar = getParagraph(h, bold = true).setFontSize(cellFontSize)
            val rowNameCell = Cell().add(headerPar)
            rowNameCell.setBorder(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
            colTable.addCell(rowNameCell)
        }

    }

    private fun addRow(colTable: Table, row: EnrichmentRow, multiTestCorr: Boolean?) {
        val pVal = if(multiTestCorr == true) row.qvalue else row.pvalue
        addStringCell(colTable, row.column ?: "", DefaultSplitCharacters())
        addStringCell(colTable, row.type ?: "", CustomSplitCharacters())
        addStringCell(colTable, row.name ?: "")
        addStringCell(colTable, row.size?.toString() ?: "", CustomSplitCharacters())
        addDoubleCell(colTable, row.score, CustomSplitCharacters())
        addDoubleCell(colTable, pVal, CustomSplitCharacters())
        addDoubleCell(colTable, row.median, CustomSplitCharacters(), scientific = false)
    }

    private fun addStringCell(colTable: Table, cellString: String, splitCharacters: DefaultSplitCharacters = DefaultSplitCharacters()) {
        val cell = Cell().add(getParagraph(cellString).setFontSize(cellFontSize).setSplitCharacters(splitCharacters))
        cell.setBorder(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
        colTable.addCell(cell)
    }

    private fun addDoubleCell(colTable: Table, cellVal: Double?, splitCharacters: DefaultSplitCharacters = DefaultSplitCharacters(), scientific: Boolean = true) {
        val n = if (cellVal == null) "" else if (cellVal == 0.0) "0" else {
            val f = if(scientific) DecimalFormat("00.##E0") else DecimalFormat("0.###")
            f.format(cellVal)
        }
        addStringCell(colTable, n, splitCharacters)
    }

}

private class CustomSplitCharacters : DefaultSplitCharacters() {
    override fun isSplitCharacter(text: GlyphLine, glyphPos: Int): Boolean {
        if (!text[glyphPos].hasValidUnicode()) {
            return false
        }
        //val baseResult = super.isSplitCharacter(text, glyphPos)
        var myResult = false
        val glyph = text[glyphPos]
        if (glyph.unicode == '_'.code) {
            myResult = true
        }
        return myResult// || baseResult
    }
}

internal class CustomCellRenderer(modelElement: Cell?) : CellRenderer(modelElement) {
    override fun layout(layoutContext: LayoutContext): LayoutResult {
        val result: LayoutResult = super.layout(layoutContext)
        if (LayoutResult.FULL !== result.getStatus()) {
            result.setStatus(LayoutResult.NOTHING)
            result.setSplitRenderer(null)
            result.setOverflowRenderer(this)
        }
        return result
    }

    override fun getNextRenderer(): IRenderer {
        return CustomCellRenderer(getModelElement() as Cell)
    }
}
