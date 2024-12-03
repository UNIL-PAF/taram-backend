package ch.unil.pafanalysis.templates.service

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepParams
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.model.AnalysisStepType
import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import ch.unil.pafanalysis.analysis.service.AnalysisService
import ch.unil.pafanalysis.analysis.service.AnalysisStepRepository
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.analysis.steps.initial_result.InitialResultRunner
import ch.unil.pafanalysis.templates.model.Template
import ch.unil.pafanalysis.templates.model.TemplateStep
import com.google.gson.Gson
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
    private var analysisStepRepo: AnalysisStepRepository? = null

    @Autowired
    private var analysisService: AnalysisService? = null

    @Autowired
    private var commonStep: CommonStep? = null

    @Autowired
    private var initialResultRunner: InitialResultRunner? = null

    private val gson = Gson()

    fun create(analysisId: Int, name: String?, description: String?): Template? {
        val analysis = analysisRepo?.findById(analysisId)
        val origSteps: List<AnalysisStep>? = analysisService?.sortAnalysisSteps(analysis?.analysisSteps)

        val templateName = (name ?: analysis?.name) ?: ("template_" + analysis?.id)
        val template = templateRepo?.save(
            Template(
                id = 0,
                name = templateName,
                lastModifDate = LocalDateTime.now(),
                description = description
            )
        )

        val templateSteps: List<TemplateStep?>? = origSteps?.map { analysisToTemplateStep(it, template) }
        templateSteps?.forEach {
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
        val updatedTemplate = when (fieldName) {
            "name" -> template?.copy(name = fieldValue)
            "description" -> template?.copy(description = fieldValue)
            else -> throw Exception("No field [$fieldName] available.")
        }
        val res = templateRepo?.save(updatedTemplate!!)
        return res != null
    }

    private fun waitTillDone(step: AnalysisStep?,) {
        var reloadedStatus: String?
        var running = true
        var emergencyBreak = 1
        while(running && emergencyBreak <= 180){
            println("check if running ${step?.id} - $emergencyBreak")
            reloadedStatus = analysisStepRepo?.getStepStatusById(step?.id!!)
            println("$reloadedStatus")
            if(reloadedStatus == AnalysisStepStatus.RUNNING.value){
                // sleep a little
                Thread.sleep(1000)
                emergencyBreak ++
            } else running = false
        }
    }

    fun runTemplate(templateId: Int, analysisId: Int): Int? {
        val analysis = analysisRepo?.findById(analysisId)
        val lastStep: AnalysisStep? =  analysisService?.sortAnalysisSteps(analysis?.analysisSteps)?.last()
        val template = templateRepo?.findById(templateId)
        println("start")

        // try to set groups and default value
        val columnMappingParams = template?.templateSteps?.first()?.parameters
        if(lastStep?.type == AnalysisStepType.INITIAL_RESULT.value && columnMappingParams !== null){
            println(columnMappingParams)
            initialResultRunner?.updateColumnParams(lastStep, columnMappingParams)
        }

        /*val nextStep =
        asyncAnaysisStepService?.setAllStepsStatus(nextStep, AnalysisStepStatus.IDLE)

         */

        template?.templateSteps?.fold(lastStep){ acc, tmpl ->
            println("template: ${tmpl.id}")
            if(tmpl.type != AnalysisStepType.INITIAL_RESULT.value){
                println("${acc?.id} - ${acc?.status}")
                val newStep = commonStep?.addStep(acc!!.id!!, AnalysisStepParams(type = tmpl.type, params = tmpl.parameters))
                println("new step: ${newStep?.id}")
                waitTillDone(newStep)
                println("run finished")
                newStep
            } else acc
        }

        println("finish")

        return analysis?.id
    }

    private fun analysisToTemplateStep(analysisStep: AnalysisStep, template: Template?): TemplateStep {
        return TemplateStep(
            id = 0,
            type = analysisStep.type,
            lastModifDate = LocalDateTime.now(),
            parameters = if(analysisStep.type == AnalysisStepType.INITIAL_RESULT.value) gson.toJson(analysisStep.columnInfo?.columnMapping) else analysisStep.parameters,
            parametersHash = if(analysisStep.type == AnalysisStepType.INITIAL_RESULT.value) null else analysisStep.parametersHash,
            template = template
        )
    }

}