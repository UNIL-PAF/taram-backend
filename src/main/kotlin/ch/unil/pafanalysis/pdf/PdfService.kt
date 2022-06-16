package ch.unil.pafanalysis.pdf

import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import ch.unil.pafanalysis.analysis.service.AnalysisService
import ch.unil.pafanalysis.analysis.steps.CommonStep
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class PdfService {

    @Autowired
    private var analysisRepo: AnalysisRepository? = null

    @Autowired
    private var analysisService: AnalysisService? = null

    @Autowired
    private var commonStep: CommonStep? = null

    fun createPdf(analysisId: Int): String {
        val analysis = analysisRepo?.findById(analysisId)
        val steps = analysisService?.sortAnalysisSteps(analysis?.analysisSteps)

        val filePath = "/tmp/a.pdf"
        val pdf = PdfDocument(PdfWriter(filePath))
        val document: Document? = Document(pdf)

        steps?.fold(document) { acc, el ->
            commonStep?.getRunner(el.type)?.createPdf(el, acc, pdf)
        }

        document?.close()

        return filePath
    }

}