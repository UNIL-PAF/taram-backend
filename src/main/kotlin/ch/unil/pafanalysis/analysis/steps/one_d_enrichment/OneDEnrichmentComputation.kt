package ch.unil.pafanalysis.analysis.steps.one_d_enrichment

import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.HeaderTypeMapping
import ch.unil.pafanalysis.common.MultipleTestingCorrection
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.results.model.ResultType
import com.google.common.math.Quantiles
import org.apache.commons.math3.stat.inference.MannWhitneyUTest
import org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest
import org.apache.commons.math3.stat.ranking.NaNStrategy
import org.apache.commons.math3.stat.ranking.TiesStrategy
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.FileReader

@Service
class OneDEnrichmentComputation() {

    private val readTableData = ReadTableData()
    private val headerTypeMapping = HeaderTypeMapping()

    fun computeEnrichment(resTable: Table?, resultType: ResultType?, params: OneDEnrichmentParams?, annotationFilePath: String?): List<EnrichmentRow>? {
        val mwTest = MannWhitneyUTest()
        val mulitTestCorr = MultipleTestingCorrection()

        val colName = params?.colName ?: throw StepException("You have to choose a valid column.")

        // prepare annotation data
        val annotationList: Map<String, List<String>>? = parseAnnotationList(annotationFilePath, params)
        val proteinMapping: Map<String, String>? = parseProteinMapping(annotationFilePath)

        // prepare result data
        val proteinAcList: List<List<String>>? =
            readTableData.getStringColumn(resTable, headerTypeMapping.getCol("proteinIds", resultType?.value))
                ?.map { it.split(";") }
        val selResVals: List<Double>? = readTableData.getDoubleColumn(resTable, colName)
        val resList = proteinAcList?.zip(selResVals!!)

        val res: List<List<EnrichmentRow>?>? = params?.categoryNames?.fold(emptyList()) { acc, category ->
            val categoryAnnotations: Map<String, List<String>> = getCategoryAnnotations(annotationFilePath, category)
            val newRows: List<EnrichmentRow>? = annotationList?.get(category)?.fold(emptyList()) { innerAcc, annot ->
                val xyLists = createXYLists(resList, proteinMapping, categoryAnnotations, annot)
                if(xyLists?.first?.size != null &&  xyLists?.first?.size > 0) {
                    val pVal = mwTest.mannWhitneyUTest(xyLists?.first?.toDoubleArray(), xyLists?.second?.toDoubleArray())
                    val mean = xyLists?.first?.average()
                    val median = Quantiles.median().compute(xyLists?.first)
                    val r1 = computeMeanRanks(xyLists.first, xyLists.second)
                    val r2 = computeMeanRanks(xyLists.second, xyLists.first)
                    val size = xyLists?.first?.size
                    val n = size + xyLists?.second?.size
                    val score = 2 * (r2 - r1) / n
                    innerAcc.plusElement(EnrichmentRow(colName, category, annot, size, score, pVal, null, mean, median))
                } else innerAcc
            }
            acc.plusElement(newRows!!)
        }

        val resWithCorr = if(params?.fdrCorrection == true){
            // compute qValues
            res?.map{ oneCat ->
                val pVals = oneCat?.map{it?.pValue ?: throw Exception("pValue cannot be null") } ?: emptyList()
                val qVals = mulitTestCorr.fdrCorrection(pVals)
                oneCat?.mapIndexed{ i, a -> a.copy(qValue = qVals[i])}
            }
        } else res

        val flatRes = resWithCorr?.flatMap { it!! }

        // filter by threshold
        return if(params.threshold != null) flatRes?.filter { a -> (a.qValue ?: a.pValue)!! <= params.threshold } else flatRes
    }

    private fun computeMeanRanks(selPart: List<Double>, otherPart: List<Double>): Double {
        val sel = selPart.map{ it to true}
        val other = otherPart.map{ it to false}
        val allSorted = sel.plus(other).mapIndexed{ i, a -> Triple(i, a.first, a.second)}.sortedBy { it.second }
        val allRanked = allSorted.reversed().mapIndexed{ i, a -> Pair(i+1, a.third)}
        return allRanked.filter{it.second}.map{it.first}.average()
    }

    private fun getCategoryAnnotations(annotationFilePath: String?, annotationName: String): Map<String, List<String>> {
        val sep = "\t"

        val reader = BufferedReader(FileReader(annotationFilePath))
        val headers: List<String> = reader.readLine().split(sep)
        val protIdx = headers.indexOf("UniProt")
        val annotIdx = headers.indexOf(annotationName)

        return reader.readLines().fold(emptyMap<String, List<String>>()) { acc, line ->
            val cols = line.split(sep)
            if(cols[0].contains("Type")) acc
            else{
                val prot = cols[protIdx].split(";")[0]
                val categories = cols[annotIdx].split(";")
                acc.plus(prot to categories)
            }
        }
    }

    private fun createXYLists(
        resList: List<Pair<List<String>, Double>>?,
        proteinMapping: Map<String, String>?,
        categoryAnnotations: Map<String, List<String>>,
        annot: String
    ): Pair<List<Double>, List<Double>>? {
        val initialVal = Pair(emptyList<Double>(), emptyList<Double>())
        return resList?.fold(initialVal) { acc, row ->
            val foundMatch = row.first.find{ ac ->
                val uniqueAc = proteinMapping?.get(ac) ?: ac
                categoryAnnotations[uniqueAc]?.contains(annot) == true
            }

            if(foundMatch != null){
                acc.first.plusElement(row.second) to acc.second
            }else{
                acc.first to acc.second.plus(row.second)
            }
        }
    }

    private fun parseProteinMapping(annotationFilePath: String?): Map<String, String>? {
        val sep = "\t"

        val reader = BufferedReader(FileReader(annotationFilePath))
        val headers: List<String> = reader.readLine().split(sep)
        val protIdx = headers.indexOf("UniProt")

        return reader.readLines().fold(emptyMap<String, String>()){ acc, line ->
            val cols = line.split(sep)
            if(cols[0].contains("Type")) acc
            else{
                val prots = cols[protIdx].split(";")
                if(prots.size > 1){
                    val to = prots.first()
                    val fromList = prots.takeLast(prots.size - 1)
                    fromList.fold(acc){ innerAcc, from ->
                        acc.plus(Pair(from, to))
                    }
                } else acc
            }
        }
    }

    private fun parseAnnotationList(annotationFilePath: String?, params: OneDEnrichmentParams?): Map<String, List<String>>? {
        val sep = "\t"

        val reader = BufferedReader(FileReader(annotationFilePath))
        val headers: List<String> = reader.readLine().split(sep)
        return reader.readLines().fold(emptyMap<String, List<String>>()){ acc, line ->
            val cols: List<String> = line.split(sep)
            // for MaxQuant files we have to remove the second line
            if(cols[0].contains("Type")) acc
            else{
                params!!.categoryNames!!.fold(acc){ innerAcc, name ->
                    val idx = headers.indexOfFirst { it == name }
                    val a: List<String> = innerAcc[name] ?: emptyList<String>()
                    innerAcc.plus(mapOf(name to a.plus(cols[idx].split(";").filter{it.isNotEmpty()}).distinct()))
                }
            }
        }
    }

}