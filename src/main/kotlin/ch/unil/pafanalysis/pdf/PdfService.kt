package ch.unil.pafanalysis.pdf

import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import ch.unil.pafanalysis.analysis.service.AnalysisService
import ch.unil.pafanalysis.analysis.steps.CommonStep
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Div
import com.itextpdf.layout.element.Paragraph
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File


@Service
class PdfService {

    @Autowired
    private var analysisRepo: AnalysisRepository? = null

    @Autowired
    private var analysisService: AnalysisService? = null

    @Autowired
    private var commonStep: CommonStep? = null

    fun createPdf(analysisId: Int): File {
        val analysis = analysisRepo?.findById(analysisId)
        val steps = analysisService?.sortAnalysisSteps(analysis?.analysisSteps)

        val filePath = kotlin.io.path.createTempFile(suffix = ".pdf").toFile()
        val pdf = PdfDocument(PdfWriter(filePath))

        val pageSize: PageSize = PageSize.A4
        val document: Document? = Document(pdf, pageSize)

        val plotWidth: Float = pageSize?.width?.minus(document?.rightMargin?: 0f)?.minus(document?.leftMargin?: 0f)

        steps?.forEachIndexed { i, step ->
            val div = commonStep?.getRunner(step.type)?.createPdf(step, pdf, plotWidth, i + 1)
            if(step.comments != null) div?.add(commentDiv(step.comments))
            div?.setBorder(SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
            div?.isKeepTogether = true
            div?.setMarginBottom(3f)
            document?.add(div)
        }

        document?.close()
        pdf.close()

        return filePath
    }

    private fun commentDiv(comment: String): Div {
        val p1 = Paragraph(comment)
        p1.setBackgroundColor(ColorConstants.YELLOW)
        p1.setFontSize(10f)
        p1.setPaddingLeft(5f)
        val div = Div()
        div.add(p1)
        return div
    }

}