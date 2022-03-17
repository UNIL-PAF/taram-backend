package ch.unil.pafanalysis.controllers

import ch.unil.pafanalysis.results.model.AvailableDir
import ch.unil.pafanalysis.results.model.Result
import ch.unil.pafanalysis.results.model.ResultPaths
import ch.unil.pafanalysis.results.service.CheckForNewDirs
import ch.unil.pafanalysis.results.service.ResultRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.web.bind.annotation.*

@CrossOrigin(origins = ["http://localhost:3000"], maxAge = 3600)
@RestController
// This means that this class is a Controller
@RequestMapping(path = ["/result"])
class ResultController {

    @Autowired
    private var resultRepository: ResultRepository? = null

    @Autowired
    private var env: Environment? = null

    private fun getResultPaths(): ResultPaths = ResultPaths(
        maxQuantPath = env?.getProperty("result.path.maxquant"),
        spectronautPath = env?.getProperty("result.path.spectronaut")
    )

    @PostMapping("/add")
    @ResponseBody
    fun addResult(@RequestBody res: Result){
        resultRepository?.save(res)
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

}