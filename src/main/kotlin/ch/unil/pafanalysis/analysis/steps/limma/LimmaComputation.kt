package ch.unil.pafanalysis.analysis.steps.limma

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.analysis.steps.StepException
import ch.unil.pafanalysis.common.ImputationTable
import ch.unil.pafanalysis.common.ReadImputationTableData
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import com.github.rcaller.rstuff.RCaller
import com.github.rcaller.rstuff.RCallerOptions
import com.github.rcaller.rstuff.RCode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

@Service
class LimmaComputation {

    @Autowired
    private var env: Environment? = null

    fun run(table: Table?, params: LimmaParams?, step: AnalysisStep?): Pair<Table?, Limma> {

        if (step?.columnInfo?.columnMapping?.experimentDetails == null || step.columnInfo.columnMapping.experimentDetails.values.any { it.isSelected == true && it.group == null }) throw StepException(
            "Please specify your groups in the Analysis parameters."
        )

        if ((params?.firstGroup?.size ?: 0) == 0) {
            throw StepException("You must at least chose one valid pair of groups in first and second group.")
        }

        if(params?.firstGroup?.size != params?.secondGroup?.size){
            throw StepException("First and second groups don't have the same number of items. Please select pairs.")
        }

        val imputationTable = if(step.imputationTablePath != null) ReadImputationTableData().getTable(
            env?.getProperty("output.path").plus(step.imputationTablePath),
            step.commonResult?.headers
        ) else null

        val comparisions: List<GroupComp>? =
            params?.firstGroup?.zip(params.secondGroup ?: emptyList())?.map { GroupComp(it.first, it.second) }
        
        val field = params?.field ?: step.columnInfo.columnMapping.intCol

        val res: Pair<Table?, Limma> = computeComparisions(step, comparisions, table, field, params, imputationTable)

        return res
    }

    private val readTableData = ReadTableData()

    private fun computeComparisions(
        step: AnalysisStep?,
        comps: List<GroupComp>?,
        table: Table?,
        field: String?,
        params: LimmaParams?,
        imputationTable: ImputationTable?
    ): Pair<Table?, Limma> {
        val expDetails = step?.columnInfo?.columnMapping?.experimentDetails
        val (header, ints) = readTableData.getDoubleMatrixByRow(table, field, expDetails)
        val groups: List<String?> = header.map{h -> expDetails?.get(h.experiment?.name)?.group}

        val limmaRes = computeLimmaR(ints, groups, comps)

        fun getValids(group: String): List<Boolean> {
            val groupIdxs = imputationTable?.headers?.withIndex()?.filter{expDetails?.get(it.value.experiment?.name)?.group == group}?.map{it.index}
            return imputationTable?.rows?.map{ row ->
                val selRow: List<Boolean>? = groupIdxs?.map{row[it] ?: false}
                (selRow?.count { !it } ?: 0) >= (params?.minNrValid ?: 0)
            } ?: emptyList()
        }

        val validMatrix: List<List<Boolean>>? = if(params?.filterOnValid == true && imputationTable != null){
            comps?.map{ comp -> getValids(comp.group1).zip(getValids(comp.group2)).map{a -> a.first || a.second} }
        } else null

        val signMatrix: List<List<Boolean>>? = if(params?.signThres != null){
            (if(params.multiTestCorr != MulitTestCorr.NONE.value) limmaRes.adjPVals else limmaRes.pVals)?.map{c -> c.map{it <= params.signThres}}
        }else null

        val (newTable, limmaComps) = (comps?: throw StepException("Error while computing Limma")).foldIndexed(Pair(table, emptyList<LimmaComparision>())){ i, acc, comp ->
            val adjPVals = if(params?.multiTestCorr != MulitTestCorr.NONE.value) limmaRes.adjPVals?.get(i) else limmaRes.adjPVals?.get(i)?.map{_ -> Double.NaN}
            addResults(
                acc,
                limmaRes.pVals?.get(i),
                 adjPVals,
                limmaRes.fc?.get(i),
                limmaRes.tVals?.get(i),
                validMatrix?.get(i),
                signMatrix?.get(i),
                comp
                )
        }

        return Pair(newTable, Limma(limmaComps))
    }

    private fun addResults(
        tableAndLimma: Pair<Table?, List<LimmaComparision>>,
        pVals: List<Double>?,
        adjPVals: List<Double>?,
        foldChanges: List<Double>?,
        tStatistics: List<Double>?,
        validList: List<Boolean>?,
        signList: List<Boolean>?,
        comp: GroupComp
    ): Pair<Table, List<LimmaComparision>> {
        val (table, limmaComps) = tableAndLimma
        val nrHeaders = table?.headers?.size!!
        val compName = "${comp.group1.trim()}-${comp.group2.trim()}"
        val pValHeader = listOf(Header(name = "p.value.$compName", idx = nrHeaders, ColType.NUMBER, Experiment(comp = comp)))
        val otherHeaders = listOf(
            Header(name = "adj.p.value.$compName", idx = nrHeaders + 1, ColType.NUMBER, Experiment(comp = comp)),
            Header(name = "log2.fold.change.$compName", idx = nrHeaders + 2, ColType.NUMBER, Experiment(comp = comp)),
            Header(name = "is.significant.$compName", idx = nrHeaders + 3, ColType.CHARACTER, Experiment(comp = comp)),
            Header(name = "t.statistic.$compName", idx = nrHeaders + 4, ColType.NUMBER, Experiment(comp = comp)),
        )
        val newHeaders: List<Header> = table.headers.plus(pValHeader).plus(otherHeaders)

        val pValCol = (pVals ?: throw StepException("P-values are missing in limma results."))
            .mapIndexed{ i, a ->  if(validList?.get(i) == false) Double.NaN else a}
        val adjPValCol = (adjPVals ?: throw StepException("Adjusted p-values are missing in limma results."))
            .mapIndexed{ i, a ->  if(validList?.get(i) == false) Double.NaN else a}
        val foldCols = (foldChanges ?: throw StepException("Fold changes are missing in limma results."))
            .mapIndexed{ i, a ->  if(validList?.get(i) == false) Double.NaN else a}
        val signCols = (signList ?: throw StepException("Sign changes are missing in limma results."))
            .mapIndexed{ i, a ->  if(validList?.get(i) == false) false else a}
        val tCols = (tStatistics ?: throw StepException("T-statistics are missing in limma results."))
            .mapIndexed{ i, a ->  if(validList?.get(i) == false) Double.NaN else a}

        val addCols: List<List<Any>> = listOf(pValCol, adjPValCol, foldCols, signCols, tCols)
        val newCols = table.cols?.plus(addCols)
        val nrSign = signCols.map { if (it) 1 else 0 }.sum()

        val newLimmaComps = limmaComps.plusElement(
            LimmaComparision(
                comp.group1,
                comp.group2,
                nrSign,
                nrPassedFilter = validList?.count{ it }
            )
        )

        return Pair(Table(newHeaders, newCols), newLimmaComps)
    }

    data class LimmaRes(
        val pVals: List<List<Double>>?,
        val adjPVals: List<List<Double>>?,
        val tVals: List<List<Double>>?,
        val fc: List<List<Double>>?
    )

    private fun computeLimmaR(ints: List<List<Double>>, groups: List<String?>, comps: List<GroupComp>?): LimmaRes {
        val myGroups: List<String> = groups.map{ it ?: throw StepException("Groups have to be defined.") }
        fun makeRName(x: String): String = 'X' + x.replace(Regex("[^0-9A-Za-z_]"), ".")
        val contrasts = comps?.joinToString(separator = ",\n") { (g1, g2) ->
            val g1R = makeRName(g1)
            val g2R = makeRName(g2)
            "${g1R}_${g2R} = $g1R - $g2R"
        }

        val code = RCode.create()
        code.R_require("limma")
        code.addDoubleMatrix("m", ints.map { it.toDoubleArray() }.toTypedArray())
        code.addStringArray("groups", myGroups.map{makeRName(it) }.toTypedArray())

        code.addRCode("""
            group_f <- factor(groups)
            design <- model.matrix(~ 0 + group_f)
            colnames(design) <- levels(group_f)
            fit <- lmFit(m, design)
            contrast <- makeContrasts(
              $contrasts,
              levels = design
            )
            fit2 <- contrasts.fit(fit, contrast)
            fit2 <- eBayes(fit2)

            p_vals <- fit2${'$'}p.value
            p_vals[is.na(p_vals)] <- NaN
            
            t_vals <- fit2${'$'}t
            t_vals[is.na(t_vals)] <- NaN
            
            fc <- fit2${'$'}coefficients
            fc[is.na(fc)] <- NaN
            
            p_adj <- apply(p_vals, 2, function(x) p.adjust(x, method="fdr"))
            
            res <- list(p_vals=t(p_vals), p_adj=t(p_adj), t_vals=t(t_vals), fc=t(fc))
        """.trimIndent())

        val caller = RCaller.create(code, RCallerOptions.create())
        caller.runAndReturnResult("res")
        val pVals = caller.parser.getAsDoubleMatrix("p_vals").map { it.toList() }
        val adjVals = caller.parser.getAsDoubleMatrix("p_adj").map { it.toList() }
        val tVals = caller.parser.getAsDoubleMatrix("t_vals").map { it.toList() }
        val fc = caller.parser.getAsDoubleMatrix("fc").map { it.toList() }

        return LimmaRes(pVals = pVals, adjPVals = adjVals, tVals = tVals, fc = fc)
    }

}