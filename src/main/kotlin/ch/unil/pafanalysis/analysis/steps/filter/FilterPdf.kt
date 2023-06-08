package ch.unil.pafanalysis.analysis.steps.filter

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.log_transformation.LogTransformationParams
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.borders.*
import com.itextpdf.layout.element.*
import org.springframework.stereotype.Service


@Service
class FilterPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, Filter::class.java)

        val stepDiv = Div()
        stepDiv.add(titleDiv("$stepNr - Filter rows", step.nrProteinGroups, step.tableNr, plotWidth))

        val colTable = Table(2)
        val tableMargin = 5f
        colTable.setWidth(plotWidth - tableMargin)

        val tableData: List<Pair<String, Paragraph?>> = listOf(
            "Rows removed" to Paragraph(res.nrRowsRemoved.toString())
        )

        val leftCell = Cell().add(addTwoRowTable(tableData))
        leftCell.setWidth(plotWidth/2)
        leftCell.setBorder(Border.NO_BORDER)
        colTable.addCell(leftCell)

        val params = getParams(step)
        val rightCell = Cell().add(params)
        rightCell.setBorder(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
        colTable.addCell(rightCell)
        colTable.setMarginRight(tableMargin)
        colTable.setMarginBottom(tableMargin)

        stepDiv.add(colTable)
        return stepDiv
    }

    private fun getParams(step: AnalysisStep): Div {
        val parsedParams = gson.fromJson(step.parameters, FilterParams::class.java)
        var pList: MutableList<Paragraph> = mutableListOf<Paragraph>()
        if(parsedParams.removeOnlyIdentifiedBySite == true) pList.add(Paragraph("Remove only-identified-by-site"))
        if(parsedParams.removeReverse == true) pList.add(Paragraph("Remove reverse hits"))
        if(parsedParams.removePotentialContaminant == true) pList.add(Paragraph("Remove potential contaminants"))
        val freeParams = getFreeParams(parsedParams.colFilters)
        freeParams?.forEach { pList.add(it) }
        return parametersDiv(pList)
    }

    private fun getFreeParams(colFilters: List<ColFilter>?): List<Paragraph>? {
        return colFilters?.map{ flt ->
            val action = if(flt.removeSelected == true) "Remove" else "Only keep"
            val p = Paragraph("$action ")
            p.add(Text(" ${flt.colName} ").setItalic())
            p.add(Text(flt.comparator.symbol))
            p.add(Text(" ${flt.compareToValue}"))
            p
        }
    }

}
