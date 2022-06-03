package ch.unil.pafanalysis.pdf

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import com.itextpdf.forms.xfdf.XfdfConstants.DEST
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class PdfService {

    @Autowired
    private var analysisRepo: AnalysisRepository? = null

    fun createPdf(analysisId: Int): String {
        val filePath = "/tmp/a.pdf"
        val pdf = PdfDocument(PdfWriter(filePath))
        val document = Document(pdf)
        val line = "Hello! Welcome to iTextPdf"
        document.add(Paragraph(line))
        document.close()

        return filePath
    }

    fun createEcharts(step: AnalysisStep): String? {
        return step.resultPath + "/echarts.svg"
    }

}