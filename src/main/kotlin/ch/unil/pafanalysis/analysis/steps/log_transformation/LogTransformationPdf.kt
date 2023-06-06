package ch.unil.pafanalysis.analysis.steps.log_transformation

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
class LogTransformationPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, document: Document?, pageSize: PageSize?, stepNr: Int): Document? {
        val res = gson.fromJson(step.results, LogTransformation::class.java)

        val stepDiv = Div()

        stepDiv.add(titleDiv("$stepNr - Log transformation", step.nrProteinGroups))

        val tableData: SortedMap<String, String?> = sortedMapOf(
            "Min" to String.format("%.2f", res.min),
            "Max" to String.format("%.2f", res.max),
            "Mean" to String.format("%.2f", res.mean),
            "Median" to String.format("%.2f", res.median),
            "Sum" to String.format("%.2f", res.sum),
            "Nr of NaN" to res.nrNans?.toString()
        )

        stepDiv.add(addTable(tableData))

        if(step.comments != null) stepDiv.add(commentDiv(step.comments))
        stepDiv.add(horizontalLineDiv())

        document?.add(stepDiv)
        return document
    }

}
