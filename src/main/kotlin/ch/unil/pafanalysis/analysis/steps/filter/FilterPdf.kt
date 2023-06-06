package ch.unil.pafanalysis.analysis.steps.filter

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.UnitValue
import org.springframework.stereotype.Service
import java.util.*


@Service
class FilterPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, document: Document?, pageSize: PageSize?, stepNr: Int): Document? {
        val res = gson.fromJson(step.results, Filter::class.java)

        val stepDiv = Div()

        stepDiv.add(horizontalLineDiv())
        stepDiv.add(titleDiv("$stepNr - Filter rows", step.nrProteinGroups))

        val tableData: SortedMap<String, String?> = sortedMapOf(
            "Rows removed" to res.nrRowsRemoved.toString()
        )

        stepDiv.add(addTable(tableData))

        if(step.comments != null) stepDiv.add(commentDiv(step.comments))

        document?.add(stepDiv)
        return document
    }

}
