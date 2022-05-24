package ch.unil.pafanalysis.controllers

import ch.unil.pafanalysis.analysis.model.Analysis
import ch.unil.pafanalysis.analysis.service.AnalysisService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@CrossOrigin(origins = ["http://localhost:3000"], maxAge = 3600)
@RestController
// This means that this class is a Controller
@RequestMapping(path = ["/analysis"])
class AnalysisController {

   // @Autowired
   // private var analysisRepository: AnalysisRepository? = null

    @Autowired
    private var analysisService: AnalysisService? = null


    @GetMapping
    fun getAnalysis(@RequestParam resultId: Int): List<Analysis>? {
        return analysisService?.getSortedAnalysisList(resultId)
    }

    /*@DeleteMapping("/{analysisId}")
    fun deleteAnalysis(@PathParam analysisId: Int): Iterable<Analysis>? {
        return analysisRepository?.deleteById(analysisId)
    }*/

    @PostMapping(path = ["/duplicate/{analysisId}"])
    fun duplicateAnalysis(@PathVariable(value = "analysisId") analysisId: Int): Analysis? {
        return analysisService?.duplicateAnalysis(analysisId = analysisId, copyAllSteps = true)
    }



}