package ch.unil.pafanalysis.controllers

import ch.unil.pafanalysis.analysis.model.Analysis
import ch.unil.pafanalysis.analysis.service.AnalysisService
import ch.unil.pafanalysis.pdf.PdfService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@CrossOrigin(origins = ["http://localhost:3000"], maxAge = 3600)
@RestController
// This means that this class is a Controller
@RequestMapping(path = ["/analysis"])
class AnalysisController {

    @Autowired
    private var analysisService: AnalysisService? = null

    @Autowired
    private var pdfService: PdfService? = null


    @GetMapping
    fun getAnalysis(@RequestParam resultId: Int): List<Analysis>? {
        return analysisService?.getSortedAnalysisList(resultId)
    }

    @DeleteMapping("/{analysisId}")
    fun deleteAnalysis(@PathVariable(value = "analysisId") analysisId: Int): Int? {
        return analysisService?.delete(analysisId)
    }

    @PostMapping(path = ["/duplicate/{analysisId}"])
    fun duplicateAnalysis(@PathVariable(value = "analysisId") analysisId: Int): Analysis? {
        return analysisService?.duplicateAnalysis(analysisId = analysisId, copyAllSteps = true)
    }

    @PostMapping(path = ["/copy/{analysisId}"])
    fun copyAnalysis(@PathVariable(value = "analysisId") analysisId: Int): Analysis? {
        return analysisService?.duplicateAnalysis(analysisId = analysisId, copyAllSteps = false)
    }

    @GetMapping(path = ["/pdf/{analysisId}"])
    fun createPdf(@PathVariable(value = "analysisId") analysisId: Int): String? {
        return pdfService?.createPdf(analysisId)
    }


}