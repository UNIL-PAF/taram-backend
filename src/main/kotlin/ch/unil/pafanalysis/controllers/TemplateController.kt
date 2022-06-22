package ch.unil.pafanalysis.controllers

import ch.unil.pafanalysis.templates.model.Template
import ch.unil.pafanalysis.templates.model.TemplateStep
import ch.unil.pafanalysis.templates.service.TemplateRepository
import ch.unil.pafanalysis.templates.service.TemplateService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*


@CrossOrigin(origins = ["http://localhost:3000"], maxAge = 3600)
@RestController
// This means that this class is a Controller
@RequestMapping(path = ["/template"])
class TemplateController {

    @Autowired
    private var templateService: TemplateService? = null

    @Autowired
    private var templateRepository: TemplateRepository? = null

    @PostMapping(path = ["/from-analysis/{analysisId}"])
    fun createTemplate(@PathVariable(value = "analysisId") analysisId: Int, @RequestParam name: String? = null): Template? {
        return templateService?.create(analysisId = analysisId, name = name)
    }

    @PostMapping(path = ["/update/{templateId}"])
    fun updateTemplate(@PathVariable(value = "templateId") templateId: Int,  @RequestParam name: String? = null, @RequestParam value: String? = null): Boolean? {
        return templateService?.update(id = templateId, fieldName = name, fieldValue = value)
    }

    @DeleteMapping(path = ["/{templateId}"])
    @ResponseBody
    fun deleteTemplate(@PathVariable(value = "templateId") templateId: Int): Int? {
        return templateService?.delete(templateId)
    }

    @GetMapping()
    fun getAllTemplates(): List<Template>? {
        return templateRepository?.findAll()?.toList()
    }

    @GetMapping(path = ["/{templateId}"])
    fun getTemplate(@PathVariable(value = "templateId") templateId: Int): Template? {
        return templateRepository?.findById(templateId)
    }

}