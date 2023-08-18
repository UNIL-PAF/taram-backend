package ch.unil.pafanalysis.analysis.steps.initial_result

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.ColumnMapping
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.DoubleBorder
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.BorderRadius
import org.springframework.stereotype.Service


@Service
class InitialResultPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val initialResult = gson.fromJson(step.results, InitialResult::class.java)
        val groupsDefined = step.columnInfo?.columnMapping?.experimentDetails?.values?.find { it.group != null } != null

        val stepDiv = Div()
        stepDiv.add(titleDiv("$stepNr - Initial result", plotWidth))

        val colTable = Table(2)
        colTable.setWidth(plotWidth)
        val cellFifth = plotWidth/5

        // Groups definitions
        stepDiv.add(getParagraph("Groups and experiments:", bold = true))
        if(groupsDefined){
            val (groupHeaders, groupRows) = getGroups(step.columnInfo?.columnMapping)
            if(groupHeaders != null && groupRows != null){
                val table = Table(groupRows[0].size).setMarginBottom(10f)
                groupHeaders.forEach{
                    val cell = Cell().add(getParagraph(it, bold = true))
                        .setBorderTop(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
                        .setBorderBottom(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
                        .setBorderLeft(DoubleBorder(ColorConstants.LIGHT_GRAY, 2f))
                        .setBorderRight(DoubleBorder(ColorConstants.LIGHT_GRAY, 2f))

                    table.addCell(cell)
                }
                groupRows.forEach { it.forEach{ v ->
                    val cell = Cell().add(getParagraph(v ?: ""))
                        .setBorderTop(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
                        .setBorderBottom(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
                        .setBorderLeft(DoubleBorder(ColorConstants.LIGHT_GRAY, 2f))
                        .setBorderRight(DoubleBorder(ColorConstants.LIGHT_GRAY, 2f))
                    table.addCell(cell)
                }}
                stepDiv.add(table)
            }
        }else{
            stepDiv.add(Paragraph("No groups are defined"))
        }

        // 1. parameters
        val leftDiv = Div()
        leftDiv.add(getTwoRowTable(listOf(Pair("Default intensity column:", step?.columnInfo?.columnMapping?.intCol ?: ""))))
        val dataTable = getDataTable(initialResult)
        leftDiv.add(getTwoRowTableWithList(dataTable))
        colTable.addCell(getDataCell(leftDiv, 4*cellFifth))

        // 3. results
        val rightDiv = Div()
        rightDiv.add(getParagraph("${step.nrProteinGroups} protein groups"))
        rightDiv.add(getParagraph("Table ${step.tableNr}", bold = true, underline = true))
        colTable.addCell(getResultCell(rightDiv, cellFifth))

        stepDiv.add(colTable)
        return stepDiv
    }

    private fun getGroups(colMapping: ColumnMapping?): Pair<List<String>?, List<List<String?>>?> {
        val listByGroups: List<Pair<String, List<String>>>? =  colMapping?.experimentNames?.fold(emptyList()){ acc, exp ->
            val details = colMapping?.experimentDetails?.get(exp)
            val newAcc = if(details?.group != null && details.group.isNotEmpty()){
                val currGroup = acc.find{it.first == details.group}
                val newGroup: Pair<String, List<String>> = if(currGroup != null){
                    currGroup.copy(second = currGroup.second.plus(exp))
                } else {
                    Pair(details.group, listOf(exp))
                }
                val fltList: List<Pair<String, List<String>>>? = acc.filter{it.first != details?.group}
                fltList?.plus(newGroup) ?: listOf(newGroup)
            }else acc
            newAcc
        }

        val nrRows = listByGroups?.map{it.second.size }?.maxOrNull()?.minus(1)
        return if(nrRows != null){
            val headers = listByGroups.map{it.first}
            val rows = (0..nrRows).map { i -> listByGroups.map{ it.second.getOrNull(i)} }
            Pair(headers, rows)
        } else Pair(null, null)
    }

    private fun getDataTable(initialResult: InitialResult): List<Pair<String, List<String>>>{
        return if(initialResult?.spectronautSetup != null){
            listOf(
                Pair("Software version: ", listOf(initialResult?.softwareVersion ?: "")),
                Pair("Analysis date: ", listOf(initialResult.spectronautSetup.analysisDate ?: "")),
                Pair("Fasta files:", initialResult.fastaFiles ?: emptyList()),
                Pair("Libraries:", initialResult.spectronautSetup.libraries?.map{it.name ?: ""} ?: emptyList())
            )
        }else{
            listOf(
                Pair("Software version: ", listOf(initialResult?.softwareVersion ?: "")),
                Pair("Fasta files:", initialResult.fastaFiles ?: emptyList())
            )
        }
    }

}
