package ch.unil.pafanalysis.results.service

import ch.unil.pafanalysis.results.model.AvailableDir
import ch.unil.pafanalysis.results.model.Result
import ch.unil.pafanalysis.results.model.ResultPaths
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.readAttributes

@Component
class CheckForNewDirs {

    companion object {
        private val maxQuantResName = "proteinGroups.txt"
        private var localResultPaths: ResultPaths? = null

        fun checkAll(existingRes: Sequence<Result>?, resultPaths: ResultPaths): List<AvailableDir>{
            localResultPaths = resultPaths
            val maxQuantRes = existingRes?.filter { it.type == "MaxQuant" }
            return checkMaxQuant(maxQuantRes)
        }

        private fun checkMaxQuant(maxQuantRes: Sequence<Result>?): List<AvailableDir>{
            val usedDirs = if(maxQuantRes == null) emptySequence<String>() else maxQuantRes!!.map { it.resFile }

            val maxQuantDirs = Files.walk(Paths.get(localResultPaths?.maxQuantPath))
                .filter(Files::isRegularFile)
                .filter{file -> file?.fileName?.name == maxQuantResName}

            val newResults = maxQuantDirs.map{
                val attr: BasicFileAttributes = it.readAttributes()
                val creationTime = LocalDateTime.ofInstant( attr.creationTime().toInstant(), ZoneId.systemDefault())
                val pathString = it.pathString.replace(localResultPaths!!.maxQuantPath!!, "")

                AvailableDir(
                    type="MaxQuant",
                    resFile = "proteinGroups.txt",
                    fileCreationDate = creationTime,
                    path = pathString,
                    alreadyUsed = usedDirs.contains(pathString)
                )
            }.toList()
            return newResults
        }
    }
}