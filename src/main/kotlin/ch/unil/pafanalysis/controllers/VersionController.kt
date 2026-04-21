package ch.unil.pafanalysis.controllers

import ch.unil.pafanalysis.common.VersionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@CrossOrigin(origins = ["http://localhost:3000", "http://taram-dev.dcsr.unil.ch", "http://taram.dcsr.unil.ch"], maxAge = 3600)
@RestController
// This means that this class is a Controller
@RequestMapping(path = ["/version"])
class VersionController {

    @Autowired
    private var versionService: VersionService? = null

    @GetMapping
    fun getVersion(): String? {
        return versionService?.version()
    }

}