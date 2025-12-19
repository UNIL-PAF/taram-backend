package ch.unil.pafanalysis.results.service

import ch.unil.pafanalysis.results.model.AvailableDir
import ch.unil.pafanalysis.results.model.Result
import ch.unil.pafanalysis.results.model.ResultPaths
import ch.unil.pafanalysis.results.model.ResultType
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.filefilter.WildcardFileFilter
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileFilter
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.FileVisitOption
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.EnumSet
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.readAttributes


@Component
class CheckForNewDirs {

    companion object {
        // the filename and a boolean indicating if it is an exact match or a pattern
        private val maxQuantResName = listOf(Pair("proteinGroups.txt", true))
        private val spectronautResName = listOf(Pair("_Report[^\\.]*.xls", false), Pair("_Report[^\\.]*.tsv", false), Pair("_Report[^\\.]*.txt", false))

        fun checkAll(resultPaths: ResultPaths): List<AvailableDir>{
            val maxQuantDirs = checkMaxQuant(resultPaths)
            val spectronautDirs = checkSpectronaut(resultPaths)
            val allDirs =  maxQuantDirs.plus(spectronautDirs)
            val distinctDirs = allDirs.distinctBy { a -> a.path + a.type }
            return distinctDirs.sortedBy { it.fileCreationDate }.reversed()
        }

        private fun checkMaxQuant(resultPaths: ResultPaths): List<AvailableDir>{
            return checkCommon(resultPaths.maxQuantPath!!, maxQuantResName, ResultType.MaxQuant)
        }

        private fun checkSpectronaut(resultPaths: ResultPaths): List<AvailableDir>{
            return checkCommon(resultPaths.spectronautPath!!, spectronautResName, ResultType.Spectronaut)
        }

        private fun checkCommon(path: String, resFileNames: List<Pair<String, Boolean>>, resType: ResultType): List<AvailableDir>{

            val matcher = FileSystems.getDefault().getPathMatcher("glob:*.{txt,tsv,xls}")
            val root: Path = Paths.get(path)
            val opts: Set<FileVisitOption> = EnumSet.noneOf(FileVisitOption::class.java)

            val dirList: MutableList<AvailableDir?> = arrayListOf()
            var currentAvailableDir: AvailableDir? = null

            fun createAvailableDir(file: Path, attrs: BasicFileAttributes?, filePath: String): AvailableDir{
                val creationeDate = LocalDateTime.ofInstant( attrs?.creationTime()?.toInstant(), ZoneId.systemDefault())
                return AvailableDir(
                    path = filePath,
                    resFileList = listOf(file.fileName.toString()),
                    fileCreationDate = creationeDate,
                    type = resType.value
                )
            }

            Files.walkFileTree(root, opts, Int.MAX_VALUE, object : SimpleFileVisitor<Path>() {

                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes?): FileVisitResult {
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult {
                    if(matcher.matches(file.fileName)){
                        val filePath = file.parent.toString().replace(path, "")

                        if(currentAvailableDir == null){
                            currentAvailableDir = createAvailableDir(file, attrs, filePath)
                        }else if(currentAvailableDir?.path != filePath){
                            dirList.add(currentAvailableDir)
                            currentAvailableDir = createAvailableDir(file, attrs, filePath)
                        }else{
                            currentAvailableDir = currentAvailableDir?.copy(resFileList = (currentAvailableDir?.resFileList?: emptyList()).plus(file.fileName.toString()))
                        }
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
                    return FileVisitResult.CONTINUE
                }
            })

            fun isFileValid(fileName: String?, resFileNames: List<Pair<String, Boolean>>): Boolean{
                return resFileNames.any { resFileName ->
                    if(resFileName.second){
                        fileName == resFileName.first
                    }else{
                        Regex(resFileName.first).containsMatchIn(fileName!!)
                    }
                }
            }

            return dirList.mapNotNull {
                val resFile = it?.resFileList?.find { a -> isFileValid(a, resFileNames) }
                it?.copy(resFile = resFile)
            }

        }

    }
}