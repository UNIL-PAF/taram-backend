package ch.unil.pafanalysis.controllers

import ch.unil.pafanalysis.analysis.hints.StepHintsService
import ch.unil.pafanalysis.analysis.model.StepHintInfo
import ch.unil.pafanalysis.analysis.service.AnalysisService
import ch.unil.pafanalysis.analysis.service.AnalysisStepService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@CrossOrigin(origins = ["http://localhost:3000", "http://taram-dev.dcsr.unil.ch", "http://taram.dcsr.unil.ch"], maxAge = 3600)
@RestController
// This means that this class is a Controller
@RequestMapping(path = ["/hints"])
class HintsController {

    @Autowired
    private var hintsService: StepHintsService? = null

    @Autowired
    private var analysisService: AnalysisService? = null

    @GetMapping
    fun getHints(@RequestParam resultId: Int): StepHintInfo? {
        val analysisGroup = analysisService?.getSortedAnalysisList(resultId)
        return hintsService?.get(resultId, analysisGroup)
    }

    @PutMapping(path = ["/switch-done/{hintId}/result-id/{resultId}"])
    fun switchHintDone(@PathVariable hintId: String, @PathVariable resultId: Int): Boolean {
        return hintsService?.switchHintDone(resultId, hintId) ?: false
    }

}