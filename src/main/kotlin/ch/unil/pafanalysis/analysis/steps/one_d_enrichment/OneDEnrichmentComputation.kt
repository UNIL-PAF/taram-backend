package ch.unil.pafanalysis.analysis.steps.one_d_enrichment

import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.analysis.steps.t_test.TTestParams
import ch.unil.pafanalysis.common.HeaderTypeMapping
import ch.unil.pafanalysis.common.MultipleTestingCorrection
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import com.github.rcaller.rstuff.RCaller
import com.github.rcaller.rstuff.RCallerOptions
import com.github.rcaller.rstuff.RCode
import com.google.common.math.Quantiles
import org.apache.commons.math3.stat.inference.MannWhitneyUTest
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.FileReader

@Service
class OneDEnrichmentComputation() {

    private val readTableData = ReadTableData()
    private val headerTypeMapping = HeaderTypeMapping()

    fun computeEnrichment(
        resTable: Table?,
        resultType: String?,
        params: OneDEnrichmentParams?,
        categoryNames: List<String>?,
        annotationFilePath: String?
    ): List<EnrichmentRow>? {
        val mwTest = MannWhitneyUTest()
        // val mulitTestCorr = MultipleTestingCorrection()

        val colIdxs = params?.colIdxs ?: throw StepException("You have to choose valid columns.")

        val proteinMapping: Map<String, String>? = parseProteinMapping(annotationFilePath)

        // prepare result data
        val proteinAcList: List<List<String>>? =
            readTableData.getStringColumn(resTable, headerTypeMapping.getCol("proteinIds", resultType))
                ?.map { it.split(";") }

        val finalRes =  colIdxs.flatMap{ colIdx ->

        val selResVals: List<Double>? = readTableData.getDoubleColumn(resTable, colIdx)
        val resListOrig = proteinAcList?.zip(selResVals!!)
        val resList = resListOrig?.filter{! it.second.isNaN()}

        val colName = (resTable?.headers?.find { it.idx == colIdx } ?: return null).name

        val res: List<List<EnrichmentRow>?>? = categoryNames?.parallelStream()?.map { category ->

            val categoryAnnotations: Map<String, List<String>> = getCategoryAnnotations(annotationFilePath, category)

            // prepare annotation data
            val annotationList: List<String>? = parseAnnotationList(annotationFilePath, category)

            val newRows: List<EnrichmentRow?>? = annotationList?.map{ annot ->
                val xyLists = createXYLists(resList, proteinMapping, categoryAnnotations, annot)
                if (xyLists?.first?.size != null && xyLists?.first?.size > 0) {
                    val pVal =
                        mwTest.mannWhitneyUTest(xyLists?.first?.toDoubleArray(), xyLists?.second?.toDoubleArray())
                    val mean = xyLists?.first?.average()
                    val median = Quantiles.median().compute(xyLists?.first)
                    val r1 = computeMeanRanks(xyLists.first, xyLists.second)
                    val r2 = computeMeanRanks(xyLists.second, xyLists.first)
                    val size = xyLists?.first?.size
                    val n = size + xyLists?.second?.size
                    val score = 2 * (r2 - r1) / n
                    EnrichmentRow(
                            null,
                            colName,
                            category,
                            annot,
                            size,
                            score,
                            pVal,
                            null,
                            mean,
                            median
                        )
                } else null
            }

            newRows?.filterNotNull()
        }?.toList()

        val resWithCorr = if (params?.fdrCorrection == true) {
            // compute qValues
            res?.map { oneCat ->
                val pVals = oneCat?.map { it?.pvalue ?: throw Exception("p-value cannot be null") } ?: emptyList()
                //val qVals = mulitTestCorr.fdrCorrection(pVals)
                val qVals = multiTestCorr(pVals)
                oneCat?.mapIndexed { i, a -> a.copy(qvalue = qVals[i]) }
            }
        } else res

        val flatRes = resWithCorr?.flatMap { it!! }

        // filter by threshold
         val ha = if (params.threshold != null) flatRes?.filter { a ->
            (a.qvalue ?: a.pvalue)!! <= params.threshold
        } else flatRes
            ha!!
        }

        return finalRes.sortedByDescending { it.median }
    }

    private fun multiTestCorr(pVals: List<Double>): List<Double> {
        val code = RCode.create()
        code.addDoubleArray("p_vals", pVals.toDoubleArray())
        code.addRCode("corr_p_vals <- p.adjust(p_vals, method='BH')")
        val caller = RCaller.create(code, RCallerOptions.create())
        caller.runAndReturnResult("corr_p_vals")
        return caller.parser.getAsDoubleArray("corr_p_vals").toList()
    }

    private fun computeMeanRanks(selPart: List<Double>, otherPart: List<Double>): Double {
        /* optimization ?
            val allEntries = selPart.plus(otherPart)
            val range = allEntries.indices.toList()
            val ranks: List<Int> = range.sortedWith{ c1, c2 -> if (allEntries[c2] < allEntries[c1]) +1 else if (allEntries.get(c2) === allEntries.get(c1)) 0 else -1 }
            val a = ranks.take(selPart.size).map{it+1}.average()
         */

        val sel = selPart.map { it to true }
        val other = otherPart.map { it to false }
        val allSorted = sel.plus(other).mapIndexed { i, a -> Triple(i, a.first, a.second) }.sortedByDescending { it.second }
        val allRanked = allSorted.mapIndexed { i, a -> Pair(i + 1, a.third) }
        return allRanked.filter { it.second }.map { it.first }.average()
    }

    private fun getCategoryAnnotations(annotationFilePath: String?, categoryName: String?): Map<String, List<String>> {
        val sep = "\t"

        fun parseLine(line: String, protIdx: Int, annotIdx: Int): Pair<String, List<String>>{
            val cols = line.split(sep)
            val prot = cols[protIdx].split(";")[0]
            val categories = cols[annotIdx].split(";")
            return prot to categories
        }

        val reader = BufferedReader(FileReader(annotationFilePath))
        val headers: List<String> = reader.readLine().split(sep)
        val protIdx = headers.indexOf("UniProt")
        val annotIdx = headers.indexOf(categoryName)

        // first line can be the type line from Perseus
        val firstLine = reader.readLine()
        val firstPair: List<Pair<String, List<String>>> = if(firstLine.contains("Type")){
            emptyList<Pair<String, List<String>>>()
        }else {
            listOf(parseLine(firstLine, protIdx, annotIdx))
        }

        val res: List<Pair<String, List<String>>?> =  reader.useLines {
            it.map{ line ->
                parseLine(line, protIdx, annotIdx)
            }.toList()
        }

        firstPair.plus(res)

        return res.filterNotNull().toMap()
    }

    private fun createXYLists(
        resList: List<Pair<List<String>, Double>>?,
        proteinMapping: Map<String, String>?,
        categoryAnnotations: Map<String, List<String>>,
        annot: String
    ): Pair<List<Double>, List<Double>>? {

        val res: List<Pair<Double?, Double?>>? = resList?.map{ row ->
            val foundMatch = row.first.find { ac ->
                val uniqueAc = proteinMapping?.get(ac) ?: ac
                categoryAnnotations[uniqueAc]?.contains(annot) == true
            }

            if (foundMatch != null) {
                Pair(row.second, null)
            } else {
                Pair(null, row.second)
            }
        }

        val unzippedRes: Pair<List<Double?>, List<Double?>>? = res?.unzip()

        return Pair(unzippedRes?.first?.filterNotNull() ?: emptyList(), unzippedRes?.second?.filterNotNull() ?: emptyList())
    }

    private fun parseProteinMapping(annotationFilePath: String?): Map<String, String>? {
        val sep = "\t"

        val reader = BufferedReader(FileReader(annotationFilePath))
        val headers: List<String> = reader.readLine().split(sep)
        val protIdx = headers.indexOf("UniProt")

        fun parseLine(line: String, acc: Map<String, String>, protIdx: Int): Map<String, String>{
            val cols = line.split(sep)
            val prots = cols[protIdx].split(";")
            return if (prots.size > 1) {
                val to = prots.first()
                val fromList = prots.takeLast(prots.size - 1)
                fromList.fold(acc) { innerAcc, from ->
                    acc.plus(Pair(from, to))
                }
            } else acc
        }

        // first line can be the "Type" line from Perseus
        val initialLine = reader.readLine()
        val initialMap = if(initialLine.contains("Type")){
            emptyMap<String, String>()
        }else{
            parseLine(initialLine, emptyMap(), protIdx)
        }

        return reader.useLines {
            it.fold(initialMap) { acc, line ->
                parseLine(initialLine, acc, protIdx)
            }
        }
    }

    private fun parseAnnotationList(
        annotationFilePath: String?,
        categoryName: String?
    ):List<String>? {
        val sep = "\t"

        val reader = BufferedReader(FileReader(annotationFilePath))
        val headers: List<String> = reader.readLine().split(sep)

        fun parseLine(line: String): List<String>{
            val cols: List<String> = line.split(sep)
            val idx = headers.indexOfFirst { it == categoryName }
            return cols[idx].split(";").filter { it.isNotEmpty() }
        }

        // first line can be the "Type" line from Perseus
        val initialLine = reader.readLine()
        val intitialList = if(initialLine.contains("Type")){
            emptyList()
        }else{
            parseLine(initialLine)
        }

        val res: List<String> = reader.useLines {
            it.map{ line ->
                parseLine(line)
            }.toList()
        }.flatMap { it }.distinct()

        return intitialList.plus(res)
    }
}