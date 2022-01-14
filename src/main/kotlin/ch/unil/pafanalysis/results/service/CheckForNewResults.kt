package ch.unil.pafanalysis.results.service

import ch.unil.pafanalysis.results.model.InitialResult
import ch.unil.pafanalysis.results.model.ResultPaths
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.io.path.readAttributes

@Component
class CheckForNewResults {

    companion object {
        private val maxQuantResName = "proteinGroups.txt"
        private var localResultPaths: ResultPaths? = null

        fun checkAll(existingRes: Sequence<InitialResult>?, resultPaths: ResultPaths): List<InitialResult>{
            localResultPaths = resultPaths
            val maxQuantRes = existingRes?.filter { it.type == "MaxQuant" }
            return checkMaxQuant(maxQuantRes)
        }

        private fun checkMaxQuant(maxQuantRes: Sequence<InitialResult>?): List<InitialResult>{
            val resNames = if(maxQuantRes == null) emptySequence<String>() else maxQuantRes!!.map { it.name }

            val newDirs = File(localResultPaths?.maxQuantPath).listFiles().filter { file ->
                file.isDirectory
                        && (! resNames.contains(file.name))
                        && File(file.absolutePath + "/" + maxQuantResName).exists()
            }

            val newResults = newDirs.map{
                val attr: BasicFileAttributes = it.toPath().readAttributes()
                val creationTime = LocalDateTime.ofInstant( attr.creationTime().toInstant(), ZoneId.systemDefault())
                InitialResult(
                    id = 0,
                    name = it.name,
                    type="MaxQuant",
                    status="available",
                    resFile = it.name + "/proteinGroups.txt",
                    fileCreationDate = creationTime,
                    lastModifDate = LocalDateTime.now()
                )
            }
            return newResults
        }
    }
}