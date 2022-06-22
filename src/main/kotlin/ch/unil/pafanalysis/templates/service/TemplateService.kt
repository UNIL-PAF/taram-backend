package ch.unil.pafanalysis.templates.service

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import ch.unil.pafanalysis.analysis.service.AnalysisService
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.templates.model.Template
import ch.unil.pafanalysis.templates.model.TemplateStep
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class TemplateService {

    @Autowired
    private var templateStepRepo: TemplateStepRepository? = null

    @Autowired
    private var templateRepo: TemplateRepository? = null

    @Autowired
    private var analysisRepo: AnalysisRepository? = null

    @Autowired
    private var analysisService: AnalysisService? = null


    fun create(analysisId: Int, name: String?): Template? {
        val analysis = analysisRepo?.findById(analysisId)
        val origSteps: List<AnalysisStep>? = analysisService?.sortAnalysisSteps(analysis?.analysisSteps)

        val templateName = (name?: analysis?.name)?: "template_" + analysis?.id
        val template = templateRepo?.save(Template(id=0, name = templateName, lastModifDate = LocalDateTime.now()))

        val templateSteps: List<TemplateStep?>? = origSteps?.map{ analysisToTemplateStep(it, template) }
        val stepsWithNextIds = setNextIds(templateSteps!!)
        stepsWithNextIds?.forEach{
            templateStepRepo?.save(it!!)!!
        }

        return templateRepo?.findById(template!!.id!!)
    }

    fun delete(id: Int): Int? {
        val template = templateRepo?.findById(id)
        template?.templateSteps?.forEach { templateStepRepo?.deleteById(it.id!!) }
        return templateRepo?.deleteById(id)
    }

    fun update(id: Int, fieldName: String?, fieldValue: String?): Boolean? {
        val template = templateRepo?.findById(id)
        val updatedTemplate = when (fieldName){
            "name" -> template?.copy(name = fieldValue)
            "description" -> template?.copy(description = fieldValue)
            else -> throw Exception("No field [$fieldName] available.")
        }
        val res = templateRepo?.save(updatedTemplate!!)
        return res != null
    }

    private fun setNextIds(templateSteps: List<TemplateStep?>?): List<TemplateStep?>? {
        val nrSteps: Int = templateSteps?.size?.minus(1)!!
        return templateSteps?.mapIndexed{ i, step ->
            if(i < nrSteps){
                step!!.copy(nextId = templateSteps[i+1]!!.id)
            }else{
                step!!
            }
        }
    }

    private fun analysisToTemplateStep(analysisStep: AnalysisStep, template: Template?): TemplateStep {
        return TemplateStep(
            id = 0,
            type = analysisStep.type,
            lastModifDate = LocalDateTime.now(),
            parameters = analysisStep.parameters,
            parametersHash = analysisStep.parametersHash,
            template = template
        )
    }

}