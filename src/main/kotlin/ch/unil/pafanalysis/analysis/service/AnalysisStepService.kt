package ch.unil.pafanalysis.analysis.service

import ch.unil.pafanalysis.analysis.model.Analysis
import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.AnalysisStepStatus
import ch.unil.pafanalysis.analysis.model.ColumnInfo
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.EchartsPlot
import ch.unil.pafanalysis.common.EchartsServer
import ch.unil.pafanalysis.common.ZipTool
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.pathString

@Service
class AnalysisStepService {

    @Autowired
    private var columnInfoRepository: ColumnInfoRepository? = null

    @Autowired
    private var analysisStepRepo: AnalysisStepRepository? = null

    @Autowired
    private var env: Environment? = null

    @Autowired
    private var commonStep: CommonStep? = null

    @Autowired
    private var asyncAnaysisStepService: AsyncAnalysisStepService? = null

    @Autowired
    private var echartsServer: EchartsServer? = null

    fun updatePlotOptions(stepId: Int, echartsPlot: EchartsPlot): String? {
        return if(analysisStepRepo?.existsById(stepId) == true){
            val step = analysisStepRepo?.findById(stepId)!!
            return commonStep?.getRunner(step?.type)?.updatePlotOptions(step, echartsPlot)
        }else null
    }

    fun duplicateAnalysisSteps(
        sortedSteps: List<AnalysisStep>,
        newAnalysis: Analysis,
        copyAllSteps: Boolean
    ) {
        var stepBefore: AnalysisStep? = null
        val fltSteps = sortedSteps.filterIndexed { i: Int, _: AnalysisStep -> i == 0 || copyAllSteps }

        val columnInfo = columnInfoRepository?.saveAndFlush(fltSteps.first().columnInfo!!.copy(id = 0))

        val copiedSteps = fltSteps.map { step: AnalysisStep ->
                stepBefore = copyAnalysisStep(analysisStep = step, stepBefore = stepBefore, newAnalysis = newAnalysis, columnInfo = columnInfo)
                stepBefore
        }

        val analysisSteps = setCorrectNextIds(copiedSteps)
        asyncAnaysisStepService?.copyDuplicatedStepFiles(analysisSteps, newAnalysis?.id)
    }

    fun setCorrectNextIds(copiedSteps: List<AnalysisStep?>): List<AnalysisStep> {
        val nrSteps = copiedSteps.size - 1
        return copiedSteps.mapIndexed { i, step ->
            if (i < nrSteps) {
                analysisStepRepo?.saveAndFlush(step!!.copy(nextId = copiedSteps[i + 1]!!.id))!!
            } else {
                step!!
            }
        }
    }

    fun copyAnalysisStep(analysisStep: AnalysisStep, stepBefore: AnalysisStep?, newAnalysis: Analysis, columnInfo: ColumnInfo?): AnalysisStep? {
        return analysisStepRepo?.saveAndFlush(
            analysisStep.copy(
                id = 0,
                analysis = newAnalysis,
                beforeId = stepBefore?.id,
                copyFromId = analysisStep.id,
                stepHash = null,
                columnInfo = columnInfo
            )
        )
    }

    fun deleteStep(stepId: Int, relinkRemaining: Boolean? = true): Int? {
        val step: AnalysisStep = analysisStepRepo?.findById(stepId)!!
        var after: AnalysisStep? = null

        if (relinkRemaining == true) {
            val before: AnalysisStep? = if (step.beforeId != null) analysisStepRepo?.findById(step.beforeId) else null
            after = if (step.nextId != null) analysisStepRepo?.findById(step.nextId) else null

            if (before != null) {
                if (after != null) {
                    analysisStepRepo?.saveAndFlush(after.copy(beforeId = before?.id))
                    analysisStepRepo?.saveAndFlush(before.copy(nextId = after.id))
                } else {
                    analysisStepRepo?.saveAndFlush(before.copy(nextId = null))
                }
            }
        }

        deleteDirectory(Path.of(getOutputRoot(step?.analysis?.result?.type)?.plus(step.resultPath)))
        val res: Int? = analysisStepRepo?.deleteById(stepId)

        if (relinkRemaining == true && after !== null) {
            asyncAnaysisStepService?.setAllStepsStatus(after, AnalysisStepStatus.IDLE)

            commonStep?.getRunner(after?.type)?.run(
                after?.beforeId!!,
                after
            )
        }

        return res
    }

    fun getOutputRoot(resultType: String?): String? {
        return env?.getProperty("output.path")
    }

    fun deleteDirectory(directory: Path?): List<Boolean> {
        return if (directory != null && directory?.exists()) {
            Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .map { it.toFile() }
                .map { it.delete() }.toList()
        } else {
            emptyList<Boolean>()
        }
    }

    fun updateComment(analysisStepId: Int, comment: String?): Boolean? {
        val analysisStep = analysisStepRepo?.findById(analysisStepId)
        val step = analysisStepRepo?.saveAndFlush(analysisStep!!.copy(comments = comment))
        return step != null
    }

    fun getTable(analysisStepId: Int): String? {
        val analysisStep = analysisStepRepo?.findById(analysisStepId)
        return env?.getProperty("output.path").plus(analysisStep?.resultTablePath)
    }

    fun getZip(stepId: Int, svg: Boolean?, png: Boolean?): String? {
        val step = analysisStepRepo?.findById(stepId)
        val resultDir = env?.getProperty("output.path").plus(step?.resultPath)
        val name = step?.id.toString()?.plus("-")?.plus(step?.type)
        val dataDir: Path = Files.createDirectories(Path("$resultDir/$name"))

        if(svg == true){
            echartsServer?.getSvgPlot(step, "${step?.resultPath}/$name/$name.svg")
        }

        if(png == true){
            echartsServer?.getPngPlot(step, "${step?.resultPath}/$name/$name.png")
        }

        return ZipTool().zipDir(dataDir.pathString, "$name.zip", resultDir, true)
    }

}