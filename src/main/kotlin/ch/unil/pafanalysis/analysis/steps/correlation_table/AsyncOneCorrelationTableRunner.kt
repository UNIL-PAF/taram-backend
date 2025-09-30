package ch.unil.pafanalysis.analysis.steps.correlation_table

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.ColumnMapping
import ch.unil.pafanalysis.analysis.model.ExpInfo
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.BufferedWriter
import java.io.FileWriter

@Service
class AsyncOneCorrelationTableRunner() : CommonStep() {

    @Autowired
    private lateinit var correlationTableComputation: CorrelationTableComputation

    @Autowired
    val comp: CorrelationTableComputation? = null

    private val readTableData = ReadTableData()

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val params = gson.fromJson(newStep?.parameters, CorrelationTableParams().javaClass)

            val outputRoot = getOutputRoot()
            val table: Table = readTableData.getTable(outputRoot + newStep?.resultTablePath, newStep?.commonResult?.headers)

            val selExpDetails = newStep?.columnInfo?.columnMapping?.experimentDetails?.filterValues{it.isSelected == true}
            val intCol = params?.column ?: newStep?.columnInfo?.columnMapping?.intCol
            
            val (headers, ints) = readTableData.getDoubleMatrix(table, intCol, selExpDetails)
            val (orderedInts, orderedHeaders) = groupByGroups(ints, headers, selExpDetails, newStep?.columnInfo?.columnMapping)

            val res = correlationTableComputation.runCorrelation(orderedInts, orderedHeaders, params)
            val resWithColors = addGroupsAndColors(res, newStep?.columnInfo?.columnMapping, selExpDetails)
            val corrTable = saveResToTable(resWithColors, newStep?.resultPath)

            newStep?.copy(
                results = gson.toJson(resWithColors?.copy(correlationTable = corrTable)),
                parameters = gson.toJson(params),
                commonResult = newStep.commonResult?.copy(headers = table.headers),
            )
        }

        tryToRun(funToRun, newStep)
    }

    private fun saveResToTable(correlationTable: CorrelationTable?, resultPath: String?): String {
        val currentDateTime: java.util.Date = java.util.Date()
        val currentTimestamp: Long = currentDateTime.time
        val fileName = "correlation_table_$currentTimestamp.txt"
        val filePath = getOutputRoot() + resultPath + "/" + fileName
        writeTable(filePath, correlationTable ?: throw StepException("No enrichment to save."))
        return fileName
    }

    private fun writeTable(filePath: String, correlationTable: CorrelationTable): String {
        val writer = BufferedWriter(FileWriter(filePath))
        val sep = "\t"

        val headers: List<String>? = if(correlationTable.groupNames != null && correlationTable.groupNames.any { it.isNotBlank() }) {
            correlationTable.groupNames.mapIndexed { i, a ->
                a + " - " + (correlationTable.experimentNames?.get(i) ?: "")
            }
        } else correlationTable.experimentNames

        writer.write(sep + headers?.joinToString(separator = sep))
        writer.newLine()

        val matrixSize = headers?.size ?: 0

        for(i in 0 until matrixSize) {
            val row = (0 until matrixSize).map { j ->
                val corr = correlationTable.correlationMatrix?.find { a -> a.x == i && a.y == j }
                corr?.v?.toString() ?: ""
            }
            writer.write(row.joinToString(sep))
            writer.newLine()
        }

        writer.close()
        return filePath
    }

    private fun groupByGroups(ints: List<List<Double>>?,
                              selHeaders: List<Header>?,
                              expDetails: Map<String, ExpInfo>?,
                              columnMapping: ColumnMapping?
                              ): Pair<List<List<Double>>?, List<Header>?> {

        if(expDetails?.values?.any{!it.group.isNullOrEmpty()} !== true) return Pair(ints, selHeaders)

        val newOrder: List<Int?>? = if(expDetails.values.all{it.idx !== null}){
            selHeaders?.map{ h -> expDetails[h.experiment?.name]?.idx}
        }else{
            val headWithIdx = selHeaders?.withIndex()
            columnMapping?.groupsOrdered?.fold(emptyList<Int>()){ acc, group ->
                val selHeads: List<Int>? = headWithIdx?.filter { h -> expDetails[h.value.experiment?.name]?.group == group }?.map{it.index}
                acc.plus(selHeads ?: emptyList())
            }
        }
        return Pair(newOrder?.map{i -> ints?.get(i!!)!!}, newOrder?.map{i -> selHeaders?.get(i!!)!!})
    }

    private fun addGroupsAndColors(correlationTable: CorrelationTable?, columnMapping: ColumnMapping?, expDetails: Map<String, ExpInfo>?): CorrelationTable? {
        val groupNames = columnMapping?.groupsOrdered ?: return correlationTable

        val groupsAndColors = groupNames.zip(groupNames.mapIndexed{ i, name ->
            val color = columnMapping.experimentDetails?.values?.find{ a -> a.group == name}?.color
            if(color.isNullOrEmpty()){
                DefaultColors.plotColors[i]
            } else color
        }).map{OneGroupAndColor(it.first, it.second)}

        val anyGroups =  correlationTable?.experimentNames?.any{!expDetails?.get(it)?.group.isNullOrEmpty()}

        return correlationTable?.copy(
            groupNames = if(anyGroups == true) correlationTable.experimentNames.map{expDetails?.get(it)?.group ?: ""} else null,
            colors = correlationTable.experimentNames?.map{getColor(it, expDetails, groupsAndColors) ?: ""},
            groupsAndColors = groupsAndColors
        )
    }

    private fun getColor(expName: String, expDetails: Map<String, ExpInfo>?, groupsAndColors: List<OneGroupAndColor>?): String? {
        val color = expDetails?.get(expName)?.color
        return if(color.isNullOrEmpty()){
            groupsAndColors?.find{it.group   == expDetails?.get(expName)?.group}?.color
        } else color
    }

}