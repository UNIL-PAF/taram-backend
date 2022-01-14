package ch.unil.pafanalysis.controllers

import ch.unil.pafanalysis.results.model.InitialResult
import ch.unil.pafanalysis.results.model.ResultPaths
import ch.unil.pafanalysis.results.service.CheckForNewResults
import ch.unil.pafanalysis.results.service.ResultRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
// This means that this class is a Controller
@RequestMapping(path = ["/result"]) // This means URL's start with /demo (after Application path)
class ResultController {

    @Autowired
    private var resultRepository: ResultRepository? = null

    @Autowired
    private var env: Environment? = null

    private fun getResultPaths(): ResultPaths = ResultPaths(
        maxQuantPath = env?.getProperty("result.path.maxquant")
    )

    @PostMapping("/add")
    fun addResult(@RequestParam name: String){
        val res = InitialResult(
            id = 0,
            name = name,
            fileCreationDate = LocalDateTime.now(),
            type = "MaxQuant"
        )
        resultRepository?.save(res)
    }


    @GetMapping("/list")
    fun listResults(): Iterable<InitialResult>? {
        return resultRepository?.findAll()
    }

    @GetMapping("/refresh")
    fun refreshResults(): Iterable<InitialResult>? {
        val existingRes = resultRepository?.findAll()
        val newRes: List<InitialResult> = CheckForNewResults.checkAll(existingRes?.asSequence(), getResultPaths())

        newRes.forEach{
            resultRepository?.save(it)
        }

        return newRes
    }

}