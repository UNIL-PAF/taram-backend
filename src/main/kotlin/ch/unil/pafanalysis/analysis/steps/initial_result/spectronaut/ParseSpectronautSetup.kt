package ch.unil.pafanalysis.analysis.steps.initial_result.spectronaut

import org.springframework.stereotype.Service
import java.io.File


@Service
class ParseSpectronautSetup() {

    fun parseSetup(setupFile: File): SpectronautSetup {
        val lines: List<String> = setupFile.readLines()

        val softwareVersion = lines.find{ s -> s.contains("^Spectronaut".toRegex())}?.trim()
        val analysisType = lines.find{ s -> s.contains("^Analysis Type".toRegex())}?.trim()?.replace("^(.+:\\s*)".toRegex(), "")
        val analysisDate = lines.find{ s -> s.contains("^Analysis Date".toRegex())}?.trim()?.replace("^(.+:\\s)".toRegex(), "")

        val runParts = getRunParts(lines.dropWhile{ !it.contains("[BEGIN-SETUP]") }.dropLastWhile { !it.contains("[END-SETUP]") })

        return SpectronautSetup(
            softwareVersion = softwareVersion,
            analysisType = analysisType,
            analysisDate = analysisDate,
            runs = runParts.map{ parseRuns(it) },
            libraries = parseLibraries(getLinesForField(runParts.first(), "Libraries Used")),
            proteinDBs = parseProteinDBs(getLinesForField(runParts.first(), "Protein Databases Used"))
        )
    }

    private fun getRunParts(lines: List<String>): List<List<String>> {
        return lines.fold(emptyList<List<String>>()){ acc, v ->
            if(v.contains("Run")){
                acc.plusElement(listOf(v.trim()))
            }else{
                if(acc.size > 0){
                    val last = acc[acc.size-1]
                    val newLast = last?.plus(v.trim())
                    acc.dropLast(1).plusElement(newLast)
                }else acc
            }
        }
    }

    private fun parseRuns(lines: List<String>): SpectronautRun {
        val name = lines.find{ s -> s.contains("^Run".toRegex())}?.trim()?.replace("^(.+:\\s*)".toRegex(), "")
        val vendor = lines.find{ s -> s.contains("Vendor:".toRegex())}?.trim()?.replace("^(.+:\\s*)".toRegex(), "")
        val fileName = lines.find{ s -> s.contains("File:".toRegex())}?.trim()?.replace("^(.+:\\s*)".toRegex(), "")
        val condition = lines.find{ s -> s.contains("Condition:".toRegex())}?.trim()?.replace("^(.+:\\s*)".toRegex(), "")
        val version = lines.find{ s -> s.contains("HTRMS Version:".toRegex())}?.trim()?.replace("^(.+:\\s*)".toRegex(), "")

        return SpectronautRun(
            name = name,
            vendor = vendor,
            fileName = fileName,
            condition = condition,
            version = version
        )
    }

    private fun getLinesForField(lines: List<String>, field: String): List<String> {
        val firstLines = lines.dropWhile { ! it.contains(field) }
        val first: String = firstLines.first()
        val idx = first.indexOfFirst { it == '─' }
        return firstLines.drop(1).takeWhile { a -> a.indexOfFirst { it == '─' } != idx  }
    }

    private fun parseLibraries(lines: List<String>): List<SpectronautLibraries> {
        val nameIdx = lines.first().indexOfFirst { it == '─' }

        return lines.fold(emptyList<SpectronautLibraries>()){ acc, v ->
            if(v.indexOfFirst { it == '─'} == nameIdx){
                val name: String = v.trim().replace("^\\W+".toRegex(), "")
                acc.plusElement(SpectronautLibraries(name = name))
            } else {
                parseLibraryLine(acc, v)
            }
        }
    }

    private fun parseLibraryLine(acc: List<SpectronautLibraries>, v: String): List<SpectronautLibraries> {
        val last = acc.last()

        val newLast = if(v.contains("File Path:")){
            last.copy(fileName = v.trim().replace(".+\\\\".toRegex(), ""))
        } else last

        return acc.dropLast(1).plusElement(newLast)
    }

    private fun parseProteinDBs(lines: List<String>): List<SpectronautProteinDB> {
        val nameIdx = lines.first().indexOfFirst { it == '─' }

        return lines.fold(emptyList<SpectronautProteinDB>()){ acc, v ->
            if(v.indexOfFirst { it == '─'} == nameIdx){
                val name: String = v.trim().replace("^\\W+".toRegex(), "")
                acc.plusElement(SpectronautProteinDB(name = name))
            } else {
                parseProteinDBs(acc, v)
            }
        }
    }

    private fun parseProteinDBs(acc: List<SpectronautProteinDB>, v: String): List<SpectronautProteinDB> {
        val last = acc.last()

        val newLast = if(v.contains("Original File:")){
            last.copy(fileName = v.trim().replace("^.+?:\\s*".toRegex(), ""))
        } else if(v.contains("Date Created:")){
            last.copy(creationDate = v.trim().replace("^.+?:\\s*".toRegex(), ""))
        } else if(v.contains("Date Modified:")){
            last.copy(modificationDate = v.trim().replace("^.+?:\\s*".toRegex(), ""))
        } else  last

        return acc.dropLast(1).plusElement(newLast)
    }

}
