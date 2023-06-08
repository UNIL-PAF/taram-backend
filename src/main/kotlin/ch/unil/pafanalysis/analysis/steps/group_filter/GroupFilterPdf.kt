package ch.unil.pafanalysis.analysis.steps.group_filter

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.steps.log_transformation.LogTransformationParams
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.borders.*
import com.itextpdf.layout.element.*
import org.springframework.stereotype.Service


@Service
class GroupFilterPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val res = gson.fromJson(step.results, GroupFilter::class.java)

        val stepDiv = Div()
        stepDiv.add(titleDiv("$stepNr - Filter on valid", step.nrProteinGroups, step.tableNr, plotWidth))

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
        val parsedParams = gson.fromJson(step.parameters, GroupFilterParams::class.java)

        val groupTxt = mapOf<String, String>("one_group" to "one group", "all_groups" to "all groups", "total" to "total")

        val myIntField = if(parsedParams.field != null) parsedParams.field else step.columnInfo?.columnMapping?.intCol

        val p = Paragraph()
        p.add(Text("Only keep rows where "))
        p.add(Text(myIntField ?: "").setItalic())
        p.add(Text(" has at least ${parsedParams.minNrValid} valid values in ${groupTxt[parsedParams.filterInGroup]}."))

        return parametersDiv(listOf(p))
    }

}
