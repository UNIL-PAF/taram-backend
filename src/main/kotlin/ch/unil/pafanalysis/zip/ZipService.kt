package ch.unil.pafanalysis.zip

import ch.unil.pafanalysis.analysis.service.AnalysisRepository
import ch.unil.pafanalysis.analysis.service.AnalysisService
import ch.unil.pafanalysis.common.ZipTool
import ch.unil.pafanalysis.pdf.PdfService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString


@Service
class ZipService {

    @Autowired
    private var analysisRepo: AnalysisRepository? = null

    @Autowired
    private var analysisService: AnalysisService? = null

    @Autowired
    private var pdfService: PdfService? = null

    fun createZip(analysisId: Int, zipSelection: ZipDataSelection): File {
        val analysis = analysisRepo?.findById(analysisId)
        val steps = analysisService?.sortAnalysisSteps(analysis?.analysisSteps)

        val pdfFile = pdfService?.createPdf(analysisId, zipSelection)

        val zipName = prettyName(analysis?.result?.name + if(analysis?.name != null) "-"+analysis?.name else "")
        val zipDir = createZipDir(zipName)

        val newPdfPath = "$zipDir/$zipName.pdf"
        pdfFile?.copyTo(File(newPdfPath))
        pdfFile?.delete()

        return File(ZipTool().zipDir(zipDir, "$zipName.zip", createTempDirectory().pathString, true))
    }

    private fun prettyName(s: String): String {
        return s.replace("\\s+".toRegex(), "-").replace("--+".toRegex(), "-")
    }

    private fun createZipDir(dirName: String): String {
        val tmpDir =  createTempDirectory()
        val zipDir = tmpDir.pathString+"/"+dirName
        File(zipDir).mkdir()
        return zipDir
    }


}

