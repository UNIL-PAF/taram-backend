package ch.unil.pafanalysis.controllers

import ch.unil.pafanalysis.analysis.model.Analysis
import ch.unil.pafanalysis.analysis.model.AnalysisGroup
import ch.unil.pafanalysis.analysis.service.AnalysisService
import ch.unil.pafanalysis.pdf.PdfService
import ch.unil.pafanalysis.zip.ZipDataSelection
import ch.unil.pafanalysis.zip.ZipService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.FileInputStream
import java.io.InputStream


@CrossOrigin(origins = ["http://localhost:3000", "http://taram-dev.dcsr.unil.ch", "http://taram.dcsr.unil.ch"], maxAge = 3600)
@RestController
// This means that this class is a Controller
@RequestMapping(path = ["/analysis"])
class AnalysisController {

    @Autowired
    private var analysisService: AnalysisService? = null

    @Autowired
    private var pdfService: PdfService? = null

    @Autowired
    private var zipService: ZipService? = null

    @GetMapping
    fun getAnalysis(@RequestParam resultId: Int): AnalysisGroup? {
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

    @PutMapping(path = ["/switch-lock/{analysisId}"])
    fun switchLock(@PathVariable(value = "analysisId") analysisId: Int): Boolean? {
        return analysisService?.switchLock(analysisId)
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

    @PostMapping(path = ["/zip/{analysisId}"])
    fun createZip(@RequestBody zipSelection: ZipDataSelection, @PathVariable(value = "analysisId") analysisId: Int): ResponseEntity<ByteArray>? {
        val zipFile =  zipService?.createZip(analysisId, zipSelection)
        val inputStream: InputStream = FileInputStream(zipFile)
        val contents = inputStream.readAllBytes()

        val headers = HttpHeaders();
        headers.contentType = MediaType.MULTIPART_FORM_DATA;
        val filename = "output.zip";
        headers.setContentDispositionFormData(filename, filename);
        headers.cacheControl = "must-revalidate, post-check=0, pre-check=0";
        val response = ResponseEntity(contents, headers, HttpStatus.OK);
        return response;
    }


}