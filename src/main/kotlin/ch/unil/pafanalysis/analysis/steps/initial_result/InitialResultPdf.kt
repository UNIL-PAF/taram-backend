package ch.unil.pafanalysis.analysis.steps.initial_result

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Text
import org.springframework.stereotype.Service


@Service
class InitialResultPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, pdf: PdfDocument?, plotWidth: Float, stepNr: Int): Div? {
        val initialResult = gson.fromJson(step.results, InitialResult::class.java)
        val groupsDefined = step.columnInfo?.columnMapping?.experimentDetails?.values?.find { it.group != null } != null

        val stepDiv = Div()
        stepDiv.add(titleDiv("$stepNr - Initial result", plotWidth))

        val colTable = Table(3)
        colTable.setWidth(plotWidth)
        val cellFifth = plotWidth/5

        // 1. parameters
        val leftDiv = Div()
        val groupTxt = if(groupsDefined) "Groups are defined" else "No groups are defined"
        leftDiv.add(getParagraph(groupTxt))
        leftDiv.add(getTwoRowTable(listOf(Pair("Default intensity column:", step?.columnInfo?.columnMapping?.intCol ?: ""))))
        colTable.addCell(getDataCell(leftDiv, 2*cellFifth))

        // 2. data
        val dataTable = getDataTable(initialResult)
        val dataDiv = Div()
        dataDiv.add(getTwoRowTableWithList(dataTable))
        colTable.addCell(getDataCell(dataDiv, 2*cellFifth))

        // 3. results
        val rightDiv = Div()
        rightDiv.add(getParagraph("${step.nrProteinGroups} protein groups"))
        rightDiv.add(getParagraph("Table ${step.tableNr}"))
        colTable.addCell(getResultCell(rightDiv, cellFifth))

        stepDiv.add(colTable)
        return stepDiv
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
