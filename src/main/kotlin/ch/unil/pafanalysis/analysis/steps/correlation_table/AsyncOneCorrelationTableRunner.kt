package ch.unil.pafanalysis.analysis.steps.correlation_table

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.annotations.model.AnnotationInfo
import ch.unil.pafanalysis.annotations.service.AnnotationRepository
import ch.unil.pafanalysis.annotations.service.AnnotationService
import ch.unil.pafanalysis.common.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service


@Service
class AsyncOneCorrelationTableRunner() : CommonStep() {

    @Autowired
    val comp: CorrelationTableComputation? = null

    @Autowired
    private var env: Environment? = null

    private val readTableData = ReadTableData()

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val params = gson.fromJson(newStep?.parameters, CorrelationTableParams().javaClass)

            val outputRoot = getOutputRoot()
            val table: Table = readTableData.getTable(outputRoot + newStep?.resultTablePath, newStep?.commonResult?.headers)



            val res = CorrelationTable()

            newStep?.copy(
                results = gson.toJson(res),
                parameters = gson.toJson(params),
                commonResult = newStep?.commonResult?.copy(headers = table.headers),
            )
        }

        tryToRun(funToRun, newStep)
    }

}