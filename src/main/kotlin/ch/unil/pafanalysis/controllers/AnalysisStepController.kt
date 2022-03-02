package ch.unil.pafanalysis.controllers

import ch.unil.pafanalysis.analysis.model.Analysis
import ch.unil.pafanalysis.analysis.service.AnalysisService
import ch.unil.pafanalysis.results.model.Result
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@CrossOrigin(origins = ["http://localhost:3000"], maxAge = 3600)
@RestController
// This means that this class is a Controller
@RequestMapping(path = ["/analysis-step"])
class AnalysisStepController {

   // @Autowired
   // private var analysisRepository: AnalysisRepository? = null

    @Autowired
    private var analysisService: AnalysisService? = null

    @PostMapping(path=["/add-to/{stepId}"])
    @ResponseBody
    fun addTo(@RequestBody stepData: String, @PathVariable(value="stepId") stepId: Int): String? {
        println(stepId)
        println(stepData)
        return "addTo finished"
    }
}