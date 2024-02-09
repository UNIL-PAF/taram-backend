package ch.unil.pafanalysis.analysis.hints

import ch.unil.pafanalysis.analysis.model.*
import ch.unil.pafanalysis.results.model.Result
import ch.unil.pafanalysis.results.model.ResultStatus
import ch.unil.pafanalysis.results.service.ResultRepository
import com.google.gson.Gson
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class StepHintsService {

    @Autowired
    private var hintsRepo: StepHintsRepository? = null

    @Autowired
    private var resultRepo: ResultRepository? = null

    fun getOrCreate(resultId: Int?, analysisList: List<Analysis>?): StepHints? {
        
        return StepHints()
    }

}