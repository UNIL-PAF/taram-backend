package ch.unil.pafanalysis.controllers

import ch.unil.pafanalysis.analysis.service.AnalysisService
import ch.unil.pafanalysis.annotations.model.AnnotationInfo
import ch.unil.pafanalysis.annotations.service.AnnotationRepository
import ch.unil.pafanalysis.annotations.service.AnnotationService
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
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId

@CrossOrigin(
    origins = ["http://localhost:3000", "http://taram-dev.dcsr.unil.ch", "http://taram.dcsr.unil.ch"],
    maxAge = 3600
)
@RestController
// This means that this class is a Controller
@RequestMapping(path = ["/annotation"])
class AnnotationController {

    @Autowired
    private var annotationService: AnnotationService? = null

    @Autowired
    private var annotationRepository: AnnotationRepository? = null

    @GetMapping("/list")
    fun listAnnotations(): Iterable<AnnotationInfo>? {
        return annotationRepository?.findAll()
    }

    @PostMapping("new")
    fun newAnnotation(@RequestParam(value = "file") file: MultipartFile,
                      @RequestParam name: String? = null,
                      @RequestParam description: String? = null){
        if(name == null || file.isEmpty) throw Exception("You must provide a file and a name.")
        annotationService?.createNewAnnotation(file, name, description)
    }

    @DeleteMapping("/{annotationId}")
    fun deleteAnnotation(@PathVariable(value = "annotationId") annotationId: Int): Int? {
        return annotationService?.delete(annotationId)
    }

    @PutMapping(path = ["/update-info/{annoId}"])
    fun updateInfo(@PathVariable(value = "annoId") annoId: Int, @RequestParam name: String, @RequestParam description: String?): String? {
        return annotationService?.setInfo(annoId = annoId, name = name, description = description)
    }

}