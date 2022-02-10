package ch.unil.pafanalysis.controllers

import ch.unil.pafanalysis.analysis.model.Analysis
import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import ch.unil.pafanalysis.analysis.service.AnalysisService
import ch.unil.pafanalysis.results.model.AvailableDir
import ch.unil.pafanalysis.results.model.InitialResult
import ch.unil.pafanalysis.results.model.ResultPaths
import ch.unil.pafanalysis.results.service.CheckForNewDirs
import ch.unil.pafanalysis.results.service.ResultRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import javax.websocket.server.PathParam

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
        return analysisService?.getByResultId(resultId)
    }

    /*@DeleteMapping("/{analysisId}")
    fun deleteAnalysis(@PathParam analysisId: Int): Iterable<Analysis>? {
        return analysisRepository?.deleteById(analysisId)
    }*/


}