package ch.unil.pafanalysis.analysis.steps.initial_result

import ch.unil.pafanalysis.analysis.service.ColumnInfoService
import ch.unil.pafanalysis.analysis.steps.initial_result.spectronaut.InitialSpectronautRunner
import ch.unil.pafanalysis.analysis.steps.initial_result.spectronaut.ParseSpectronautSetup
import ch.unil.pafanalysis.results.model.ResultType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File


@SpringBootTest
class InitialResultRunnerTests {

    @Autowired
    private val spectronautRunner: InitialSpectronautRunner? = null

    @Autowired
    private val spectronautSetupParser: ParseSpectronautSetup? = null

    @Autowired
    private var columnInfoService: ColumnInfoService? = null

    @Test
    fun testMatchSpectronautGroupsWithLiu() {
        val resPath = "./src/test/resources/results/spectronaut/Liu-17984-90/"
        val filePath = File(resPath + "20240827_Liu-17984-90-directDIA-Peptides_Report.tsv")
        val setupPath = File(resPath + "20240827_Liu-17984-90-directDIA-Peptides_Report.setup.txt")

        val spectronautSetup = spectronautSetupParser?.parseSetup(setupPath)
        val (columnInfo, _) =
            columnInfoService?.createColumnInfo(filePath.path, resPath, ResultType.Spectronaut)!!

        val newColumnInfo = spectronautRunner?.matchSpectronautGroups(columnInfo, spectronautSetup!!)
        val selExp = newColumnInfo?.columnMapping?.experimentDetails?.values?.find{it.name == "17985"}

        assert(selExp?.group == "5191")
    }

}
