package ch.unil.pafanalysis.analysis.steps.boxplot

import ch.unil.pafanalysis.analysis.model.AnalysisStep
import ch.unil.pafanalysis.analysis.model.ExpInfo
import ch.unil.pafanalysis.analysis.steps.CommonStep
import ch.unil.pafanalysis.common.HeaderTypeMapping
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import com.google.common.math.Quantiles
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import kotlin.math.log2
import kotlin.random.Random

@Service
class AsyncBoxPlotRunner() : CommonStep() {

    private val readTableData = ReadTableData()
    private val hMap = HeaderTypeMapping()

    @Async
    fun runAsync(oldStepId: Int, newStep: AnalysisStep?) {
        val funToRun: () -> AnalysisStep? = {
            val boxplot = createBoxplotObj(newStep)
            newStep?.copy(
                results = gson.toJson(boxplot),
            )
        }
        tryToRun(funToRun, newStep)
    }

    private fun createBoxplotObj(analysisStep: AnalysisStep?): BoxPlot {
        val expDetailsTable = analysisStep?.columnInfo?.columnMapping?.experimentNames?.map { name ->
            analysisStep?.columnInfo?.columnMapping?.experimentDetails?.get(name)
        }?.filter { it?.isSelected ?: false }

        val experimentNames = expDetailsTable?.map { it?.name!! }
        val groupedExpDetails: Map<String?, List<ExpInfo?>>? = expDetailsTable?.groupBy { it?.group }
        val params = gson.fromJson(analysisStep?.parameters, BoxPlotParams().javaClass)

        val table = readTableData.getTable(
            getOutputRoot().plus(analysisStep?.resultTablePath),
            analysisStep?.commonResult?.headers
        )
        val intCol = params?.column ?: analysisStep?.columnInfo?.columnMapping?.intCol
        val groupNames = analysisStep?.columnInfo?.columnMapping?.groupsOrdered ?: groupedExpDetails?.keys

        val boxplotGroupData = if(groupNames?.size?:0 > 0)
                groupNames?.map { createBoxplotGroupData(it, table, intCol, analysisStep?.columnInfo?.columnMapping?.experimentDetails) }
            else  listOf(createBoxplotGroupData(null, table, intCol, analysisStep?.columnInfo?.columnMapping?.experimentDetails))

        val selExpDetails = analysisStep?.columnInfo?.columnMapping?.experimentDetails?.filterValues{it.isSelected == true}
        val selProtData = getSelProtData(table, intCol, params, analysisStep?.analysis?.result?.type, experimentNames, selExpDetails)

        return BoxPlot(
            experimentNames = experimentNames,
            boxPlotData = boxplotGroupData?.filterNotNull(),
            selProtData = selProtData,
            allProtData = if(params?.showAll == true) createAllData(table, intCol, selExpDetails) else null
        )
    }

    private fun getSelProtData(table: Table?, intCol: String?, params: BoxPlotParams?, resType: String?, expNames: List<String>?, expDetails: Map<String, ExpInfo>?): List<SelProtData>? {
        if (params?.selProts == null) return null
        val (headers, intMatrix) = readTableData.getDoubleMatrix(table, intCol, expDetails)

        val colOrder = expNames?.map{ n -> headers.indexOf(headers.find{it.experiment?.name == n}) }
        val orderById = colOrder?.withIndex()?.associate { (index, it) -> it to index }
        val sortedIntMatrix = intMatrix.withIndex().sortedBy { (index, _) -> orderById?.get(index) }.map{it.value}

        val protGroup = readTableData.getStringColumn(table, hMap.getCol("proteinIds", resType))?.map { it.split(";")[0] }
        val geneCol = readTableData.getStringColumn(table, hMap.getCol("geneNames", resType))
        val genes = geneCol?.map { it.split(";").get(0) }
        val geneNr = geneCol?.map { it.split(";").size }

        val selProts = params.selProts.map { p ->
            val i = protGroup?.indexOf(p)
            if(i != null && i >= 0){
                val ints = sortedIntMatrix.map { if (it[i].isNaN()) null else it[i] }
                val logInts = ints.map { if (it != null && !it.isNaN() && it > 0.0) log2(it) else null }

                val gene = genes?.get(i)
                val multipleGeneNames = (((geneNr?.get(i) ?: 0) > 1))
                SelProtData(prot = p, ints = ints, logInts = logInts, gene = gene, multiGenes = multipleGeneNames)
            }else{
                null
            }
        }
        return selProts.filterNotNull().ifEmpty { null }?.sortedByDescending { a -> a.ints?.map{d -> d ?: 0.0 }?.average()}
    }

    private fun createAllData(
        table: Table?,
        intCol: String?,
        selExpDetails: Map<String, ExpInfo>?
    ): List<List<AllProtPoint>>? {
        val generator = Random(10)

        val (headers, allData) = readTableData.getDoubleMatrix(table, intCol, selExpDetails)

        return allData.map{ a ->
            val yPoints = a.filter{a -> !a.isNaN() && !a.isNaN() }
            yPoints.map{ b -> AllProtPoint(y=b, j=generator.nextDouble()-0.5) }
        }
    }

    private fun createBoxplotGroupData(
        group: String?,
        table: Table?,
        intCol: String?,
        expDetails: Map<String, ExpInfo>?
    ): BoxPlotGroupData? {
        if(expDetails?.entries?.any{it.value.group == group} == false) return null

        val (headers, ints) = readTableData.getDoubleMatrix(table, intCol, expDetails, group)

        val listOfBoxplots =
            headers.mapIndexed { i, h ->
                BoxPlotData(
                    h.experiment?.name,
                    computeBoxplotData(ints[i], false),
                    computeBoxplotData(ints[i], true)
                )
            }

        return BoxPlotGroupData(group = group, groupData = listOfBoxplots)
    }


    private fun computeBoxplotData(ints: List<Double>, logScale: Boolean?): List<Double>? {
        val normInts = if (logScale != false) {
            ints.filter { it != 0.0 && !it.isNaN() }.map { log2(it) }
        } else {
            ints
        }

        val intsFlt = normInts.filter { !it.isNaN() }

        val min = intsFlt.minOrNull()!!
        val q25: Double = Quantiles.percentiles().index(25).compute(intsFlt)
        val q50: Double = Quantiles.percentiles().index(50).compute(intsFlt)
        val q75: Double = Quantiles.percentiles().index(75).compute(intsFlt)
        val max = intsFlt.maxOrNull()!!
        return listOf(min, q25, q50, q75, max)
    }

}