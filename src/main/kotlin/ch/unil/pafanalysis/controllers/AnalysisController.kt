package ch.unil.pafanalysis.controllers

import ch.unil.pafanalysis.analysis.model.Analysis
import ch.unil.pafanalysis.analysis.model.AnalysisStepParams
import ch.unil.pafanalysis.analysis.service.AnalysisService
import ch.unil.pafanalysis.pdf.PdfService
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.File
import java.io.FileInputStream
import java.io.InputStream


@CrossOrigin(origins = ["http://localhost:3000", "https://paf-analysis.dcsr.unil.ch"], maxAge = 3600)
@RestController
// This means that this class is a Controller
@RequestMapping(path = ["/analysis"])
class AnalysisController {

    @Autowired
    private var analysisService: AnalysisService? = null

    @Autowired
    private var pdfService: PdfService? = null

    @GetMapping
    fun getAnalysis(@RequestParam resultId: Int): Pair<List<Analysis>?, String?>? {
        return analysisService?.getSortedAnalysisList(resultId)
    }

    @DeleteMapping("/{analysisId}")
    fun deleteAnalysis(@PathVariable(value = "analysisId") analysisId: Int): Int? {
        return analysisService?.delete(analysisId)
    }

    @PostMapping(path = ["/duplicate/{analysisId}"])
    fun duplicateAnalysis(@PathVariable(value = "analysisId") analysisId: Int): Analysis? {
        return  analysisService?.duplicateAnalysis(analysisId = analysisId, copyAllSteps = true)
    }

    @PostMapping(path = ["/copy/{analysisId}"])
    fun copyAnalysis(@PathVariable(value = "analysisId") analysisId: Int): Analysis? {
        return analysisService?.duplicateAnalysis(analysisId = analysisId, copyAllSteps = false)
    }

    @PostMapping(path = ["/set-name/{analysisId}"])
    fun copyAnalysis(@PathVariable(value = "analysisId") analysisId: Int, @RequestBody analysisName: String): String? {
        return analysisService?.setName(analysisId, analysisName.drop(1).dropLast(1))
    }

    @GetMapping(path = ["/pdf/{analysisId}"])
    fun createPdf(@PathVariable(value = "analysisId") analysisId: Int): ResponseEntity<ByteArray>? {
        val pdfFile =  pdfService?.createPdf(analysisId)
        val inputStream: InputStream = FileInputStream(pdfFile)
        val contents = inputStream.readAllBytes()

        val headers = HttpHeaders();
        headers.contentType = MediaType.APPLICATION_PDF;
        val filename = "output.pdf";
        headers.setContentDispositionFormData(filename, filename);
        headers.cacheControl = "must-revalidate, post-check=0, pre-check=0";
        val response = ResponseEntity(contents, headers, HttpStatus.OK);
        return response;
    }


}