package ch.unil.pafanalysis.analysis.steps.initial_result

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.UnitValue
import org.springframework.stereotype.Service


@Service
class InitialResultPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, document: Document?, pageSize: PageSize?, stepNr: Int): Document? {
        val initialResult = gson.fromJson(step.results, InitialResult::class.java)

        document?.add(titleDiv("$stepNr - Initial result", initialResult.nrProteinGroups))
        document?.add(horizontalLineDiv())

        document?.add(addTabbedText("Selected intensity column:", step?.columnInfo?.columnMapping?.intCol))
        document?.add(addTabbedTextList("Fasta files:", initialResult.fastaFiles ?: emptyList()))
        document?.add(addTabbedText("Software version:", initialResult.softwareVersion))

        if(step.comments != null) document?.add(commentDiv(step.comments))
        return document
    }

}
