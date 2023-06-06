package ch.unil.pafanalysis.analysis.steps.log_transformation

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.pdf.PdfCommon
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.UnitValue
import org.springframework.stereotype.Service


@Service
class LogTransformationPdf() : PdfCommon() {

    fun createPdf(step: AnalysisStep, document: Document?, pageSize: PageSize?, stepNr: Int): Document? {
        println(step.results)

        val res = gson.fromJson(step.results, LogTransformation::class.java)

        document?.add(titleDiv("$stepNr - Log transformation", step.nrProteinGroups))
        document?.add(horizontalLineDiv())

        document?.add(addTabbedText("Min:", res.min?.toString()))
        document?.add(addTabbedText("Max:", res.max?.toString()))
        document?.add(addTabbedText("Mean:", res.mean?.toString()))
        document?.add(addTabbedText("Median:", res.median?.toString()))
        document?.add(addTabbedText("Sum:", res.sum?.toString()))
        document?.add(addTabbedText("Nr of NaN:", res.nrNans?.toString()))

        if(step.comments != null) document?.add(commentDiv(step.comments))
        return document
    }

}
