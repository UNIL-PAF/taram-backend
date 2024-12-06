package ch.unil.pafanalysis.templates.service

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.service.*
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.initial_result.InitialResultRunner
import ch.unil.pafanalysis.templates.model.Template
import ch.unil.pafanalysis.templates.model.TemplateStep
import com.google.gson.Gson
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
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

    @Autowired
    private var asyncAnaysisStepService: AsyncAnalysisStepService? = null

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

    private fun waitTillDone(step: AnalysisStep?,): AnalysisStep? {
        var reloadedStatus: String?
        var running = true
        var emergencyBreak = 1
        while(running && emergencyBreak <= 180){
            reloadedStatus = analysisStepRepo?.getStepStatusById(step?.id!!)
            if(reloadedStatus == AnalysisStepStatus.RUNNING.value){
                // sleep a little
                Thread.sleep(1000)
                emergencyBreak ++
            } else running = false
        }

        return analysisStepRepo?.getStepById(step?.id!!)
    }

    @Async
    fun runTemplate(templateId: Int, analysisId: Int): Int? {
        val analysis = analysisRepo?.findById(analysisId)
        analysisRepo?.saveAndFlush(analysis?.copy(status = AnalysisStatus.RUNNING.value)!!)

        val lastStep: AnalysisStep? =  analysisService?.sortAnalysisSteps(analysis?.analysisSteps)?.last()
        val template = templateRepo?.findById(templateId)

        // try to set groups and default value
        val columnMappingParams = template?.templateSteps?.first()?.parameters
        if(lastStep?.type == AnalysisStepType.INITIAL_RESULT.value && columnMappingParams !== null){
            initialResultRunner?.updateColumnParams(lastStep, columnMappingParams)
        }

        template?.templateSteps?.fold(lastStep){ acc, tmpl ->
            if(tmpl.type != AnalysisStepType.INITIAL_RESULT.value){
                val newStep = commonStep?.addStep(acc!!.id!!, AnalysisStepParams(type = tmpl.type, params = tmpl.parameters))
                val doneStep = waitTillDone(newStep)
                doneStep
            } else acc
        }

        // rechange analysis status
        val newAnalysis = analysisRepo?.findById(analysisId)
        analysisRepo?.saveAndFlush(newAnalysis?.copy(status = AnalysisStatus.IDLE.value)!!)

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