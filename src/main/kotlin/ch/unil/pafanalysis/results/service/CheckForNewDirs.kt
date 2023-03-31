package ch.unil.pafanalysis.results.service

import ch.unil.pafanalysis.results.model.AvailableDir
import ch.unil.pafanalysis.results.model.Result
import ch.unil.pafanalysis.results.model.ResultPaths
import ch.unil.pafanalysis.results.model.ResultType
import org.apache.commons.io.FilenameUtils
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.regex.Pattern
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.readAttributes

@Component
class CheckForNewDirs {

    companion object {
        // the filename and a boolean indicating weither it is an exact match or a pattern
        private val maxQuantResName = listOf(Pair("proteinGroups.txt", true))
        private val spectronautResName = listOf(Pair("_Report\\.txt", false), Pair("_Report[^\\.].*\\.txt", false))

        private var localResultPaths: ResultPaths? = null

        fun checkAll(existingRes: Sequence<Result>?, resultPaths: ResultPaths): List<AvailableDir>{
            localResultPaths = resultPaths
            val maxQuantDirs = checkMaxQuant(existingRes)
            val spectronautDirs = checkSpectronaut(existingRes)
            val allDirs =  maxQuantDirs.plus(spectronautDirs)
            return allDirs.sortedBy { it.fileCreationDate }.reversed()
        }

        private fun checkMaxQuant(existingRes: Sequence<Result>?): List<AvailableDir>{
            val maxQuantRes = existingRes?.filter { it.type == "MaxQuant" }
            return checkCommon(maxQuantRes, localResultPaths!!.maxQuantPath!!, maxQuantResName, ResultType.MaxQuant)

        }

        private fun checkSpectronaut(existingRes: Sequence<Result>?): List<AvailableDir>{
            val spectronautRes = existingRes?.filter { it.type == "Spectronaut" }
            return checkCommon(spectronautRes, localResultPaths!!.spectronautPath!!, spectronautResName, ResultType.Spectronaut)
        }

        private fun checkCommon(usedDirs: Sequence<Result>?, path: String, resFileNames: List<Pair<String, Boolean>>, resType: ResultType): List<AvailableDir>{
            val usedDirsPath = if(usedDirs == null) emptySequence<String>() else usedDirs!!.map { it.path }

            fun isFileValid(fileName: String?, resFileName: Pair<String, Boolean>): Boolean{
                return if(resFileName.second){
                    fileName == resFileName.first
                }else{
                    Regex(resFileName.first).containsMatchIn(fileName!!)
                }
            }

            val availableDirs = Files.walk(Paths.get(path))
                .filter(Files::isRegularFile)
                .filter{file ->
                    val fileName: String? = file?.fileName?.name
                    resFileNames.find{resFile -> isFileValid(fileName, resFile)} != null
                }

            val newResults = availableDirs.map{
                val attr: BasicFileAttributes = it.readAttributes()
                val creationTime = LocalDateTime.ofInstant( attr.creationTime().toInstant(), ZoneId.systemDefault())
                val pathString = it.pathString.replace(path, "")
                val dirString = FilenameUtils.getPath(pathString)
                val fileString = FilenameUtils.getBaseName(pathString) + '.' + FilenameUtils.getExtension(pathString)

                AvailableDir(
                    type=resType.value,
                    resFile = fileString,
                    fileCreationDate = creationTime,
                    path = dirString,
                    alreadyUsed = usedDirsPath.contains(dirString)
                )
            }.toList()
            return newResults
        }

    }
}