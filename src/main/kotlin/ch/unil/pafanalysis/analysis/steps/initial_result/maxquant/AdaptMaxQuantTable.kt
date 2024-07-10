package ch.unil.pafanalysis.analysis.steps.initial_result.maxquant

import ch.unil.pafanalysis.analysis.model.ColType
import ch.unil.pafanalysis.analysis.model.Header
import ch.unil.pafanalysis.common.HeaderTypeMapping
import ch.unil.pafanalysis.common.ReadTableData
import ch.unil.pafanalysis.common.Table
import ch.unil.pafanalysis.results.model.ResultType


object AdaptMaxQuantTable {

    private val readTable = ReadTableData()

    private val fastaGeneRegex = Regex("(?<=GN=)(.+?)(?=$|\\s)")
    private val fastaTypeRegex = Regex("^((tr)|(sp))")
    private val fastaProtRegex = Regex("")

    fun adaptTable(table: Table?): Table? {
        val fastaHeaders = readTable.getStringColumn(table, "Fasta.headers")
        val genes = readTable.getStringColumn(table, HeaderTypeMapping().getCol("geneNames", ResultType.MaxQuant.value))

        val newTable = if (fastaHeaders != null) {
            if (genes == null) parseAllGenesFromFasta(table, fastaHeaders) else completeGenesFromFasta(
                table,
                genes,
                fastaHeaders
            )
        } else table

        return newTable
    }

    private fun parseAllGenesFromFasta(table: Table?, fastaHeaders: List<String>): Table? {
        val genes = fastaHeaders.map { fasta ->
            val all = fasta.split(";")
            val types = all.map{
                val matches = fastaTypeRegex.find(fasta)
                matches?.groups?.map{it?.value}
            }

            println(fasta)

            fastaGeneRegex.find(fasta)?.value ?: ""
        }

        val headers = table?.headers
        val fastaHeaderIdx = headers?.map{it.name}?.indexOf("Fasta.headers") ?: 4
        // insert gene header just before fasta
        val geneHeader = Header(
            name = HeaderTypeMapping().getCol("geneNames", ResultType.MaxQuant.value),
            idx = 0,
            type = ColType.CHARACTER
        )
        val newHeaders = headers?.take(fastaHeaderIdx)?.plus(geneHeader)?.plus(headers?.drop(fastaHeaderIdx))?.mapIndexed { i, h ->  h.copy(idx = i)}
        val newCols = table?.cols?.take(fastaHeaderIdx)?.plusElement(genes)?.plus(table?.cols?.drop(fastaHeaderIdx))

        return Table(headers = newHeaders, cols = newCols)
    }

    private fun completeGenesFromFasta(table: Table?, genes: List<String>, fastaHeader: List<String>): Table? {
        val geneIdx = table?.headers?.map{it.name}?.indexOf(HeaderTypeMapping().getCol("geneNames", ResultType.MaxQuant.value))!!
        val newGenes = genes.zip(fastaHeader).map{ (gene, fasta) ->
            if(gene.isEmpty()){
                fastaGeneRegex.find(fasta)?.value ?: ""
            } else gene
        }

        val newCols = table?.cols?.take(geneIdx)?.plusElement(newGenes)?.plus(table?.cols?.drop(geneIdx+1))
        return table.copy(cols = newCols)
    }
}
