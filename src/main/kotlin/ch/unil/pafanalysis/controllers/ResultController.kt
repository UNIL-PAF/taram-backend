package ch.unil.pafanalysis.controllers

import ch.unil.pafanalysis.analysis.service.AnalysisService
import ch.unil.pafanalysis.results.model.AvailableDir
import ch.unil.pafanalysis.results.model.Result
import ch.unil.pafanalysis.results.model.ResultPaths
import ch.unil.pafanalysis.results.model.ResultStatus
import ch.unil.pafanalysis.results.service.CheckForNewDirs
import ch.unil.pafanalysis.results.service.ResultRepository
import ch.unil.pafanalysis.results.service.ResultService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@CrossOrigin(origins = ["http://localhost:3000", "http://taram-dev.dcsr.unil.ch", "http://taram.dcsr.unil.ch"], maxAge = 3600)
@RestController
// This means that this class is a Controller
@RequestMapping(path = ["/result"])
class ResultController {

    @Autowired
    private var resultRepository: ResultRepository? = null

    @Autowired
    private var resultService: ResultService? = null

    @Autowired
    private var analysisService: AnalysisService? = null

    @Autowired
    private var env: Environment? = null

    private fun getResultPaths(): ResultPaths = ResultPaths(
        maxQuantPath = env?.getProperty("result.path.maxquant"),
        spectronautPath = env?.getProperty("result.path.spectronaut")
    )

    @PostMapping("/add")
    @ResponseBody
    fun addResult(@RequestBody res: Result): Int?{
        val newRes = res.copy(lastModifDate = LocalDateTime.now(), status = ResultStatus.RUNNING.value)
        val newResId = resultRepository?.save(newRes)?.id

        // create new Analysis
        analysisService?.createNewAnalysis(newRes)

        return newResId
    }

    @GetMapping("/list")
    fun listResults(): Iterable<Result>? {
        return resultRepository?.findAll()
    }

    @GetMapping("/available-dirs")
    fun availableDirs(): Iterable<AvailableDir>? {
        val existingRes = resultRepository?.findAll()
        return CheckForNewDirs.checkAll(existingRes?.asSequence(), getResultPaths())
    }

    @DeleteMapping("/{resultId}")
    fun deleteResult(@PathVariable(value = "resultId") resultId: Int): Int? {
        return resultService?.delete(resultId)
    }

    @PutMapping(path = ["/update-info/{resultId}"])
    fun updateInfo(@PathVariable(value = "resultId") resultId: Int, @RequestParam name: String, @RequestParam description: String?): String? {
        return resultService?.setInfo(resultId = resultId, name = name, description = description)
    }

}