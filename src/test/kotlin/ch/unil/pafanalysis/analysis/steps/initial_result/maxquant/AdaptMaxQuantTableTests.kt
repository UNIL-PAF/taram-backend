package ch.unil.pafanalysis.analysis.steps.initial_result.maxquant

import ch.unil.pafanalysis.analysis.model.ColType
import ch.unil.pafanalysis.analysis.service.ColumnMappingParser
import ch.unil.pafanalysis.common.HeaderTypeMapping
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.results.model.ResultType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File


@SpringBootTest
class AdaptMaxQuantTableTests {

    val readTableData = ReadTableData()

    @Autowired
    val columnMappingParser: ColumnMappingParser? = null

    @Test
    fun tomczynskaTable() {
        val resPath = "./src/test/resources/results/maxquant/Tomczynska_17742-45/"
        val filePath = resPath + "proteinGroups.txt"
        val (_, commonRes)  = columnMappingParser?.parse(filePath, resPath, ResultType.MaxQuant)!!

        val resTable = readTableData.getTable(filePath, commonRes?.headers)
        val newTable = AdaptMaxQuantTable.adaptTable(resTable)

        assert(resTable?.headers?.size?.plus(2) == newTable?.headers?.size)
        assert(resTable?.cols?.size?.plus(2) == newTable?.cols?.size)

        val geneHeader = newTable?.headers?.find{it.name == HeaderTypeMapping().getCol("geneNames", ResultType.MaxQuant.value)}
        assert(geneHeader?.name == "Gene.names")

        val geneCol = newTable?.cols?.get(geneHeader?.idx!!)
        assert(geneCol?.first() as? String  == "dnaK")
        assert(geneCol?.get(9) as? String == "AGR4C_Lc50272")

        val protHeader = newTable?.headers?.find{it.name == HeaderTypeMapping().getCol("description", ResultType.MaxQuant.value)}
        assert(protHeader?.name == "Protein.names")

        val protCol = newTable?.cols?.get(protHeader?.idx!!)
        assert(protCol?.first() as? String  == "Chaperone protein DnaK")
        assert(protCol?.get(9) as? String == "Putative transcriptional regulator protein, LacI family")
    }

    @Test
    fun scherlerTable() {
        val resPath = "./src/test/resources/results/maxquant/Scherler_17824-25_with_genes/"
        val filePath = resPath + "proteinGroups.txt"
        val (_, commonRes)  = columnMappingParser?.parse(filePath, resPath, ResultType.MaxQuant)!!

        val resTable = readTableData.getTable(filePath, commonRes?.headers)
        val newTable = AdaptMaxQuantTable.adaptTable(resTable)

        assert(resTable?.headers?.size == newTable?.headers?.size)
        assert(resTable?.cols?.size == newTable?.cols?.size)

        val geneHeader = newTable?.headers?.find{it.name == HeaderTypeMapping().getCol("geneNames", ResultType.MaxQuant.value)}
        assert(geneHeader?.name == "Gene.names")
        val geneCol = newTable?.cols?.get(geneHeader?.idx!!)
        assert(geneCol?.first() as? String  == "Ddx17")
        assert(geneCol?.get(26) as? String == "Pdlim3")

        val protHeader = newTable?.headers?.find{it.name == HeaderTypeMapping().getCol("description", ResultType.MaxQuant.value)}
        assert(protHeader?.name == "Protein.names")
        val protCol = newTable?.cols?.get(protHeader?.idx!!)
        assert(protCol?.first() as? String  == "DEAD box helicase 17;RNA helicase")
        assert(protCol?.get(1) as? String == "Troponin I, slow skeletal muscle")
        assert(protCol?.get(26) as? String == "PDZ and LIM domain protein 3")
    }

    @Test
    fun bernardTable() {
        val resPath = "./src/test/resources/results/maxquant/Bernard_exp_2605/"
        val filePath = resPath + "proteinGroups.txt"
        val (_, commonRes) = columnMappingParser?.parse(filePath, resPath, ResultType.MaxQuant)!!

        val resTable = readTableData.getTable(filePath, commonRes?.headers)
        val newTable = AdaptMaxQuantTable.adaptTable(resTable)

        assert(resTable?.headers?.size == newTable?.headers?.size)
        assert(resTable?.cols?.size == newTable?.cols?.size)

        val geneHeader =
            newTable?.headers?.find { it.name == HeaderTypeMapping().getCol("geneNames", ResultType.MaxQuant.value) }
        assert(geneHeader?.name == "Gene.names")

        val geneCol = newTable?.cols?.get(geneHeader?.idx!!)
        assert(geneCol?.first() as? String == "IGHV3-72")
        assert(geneCol?.get(1) as? String == "GATD3A")
    }

}
