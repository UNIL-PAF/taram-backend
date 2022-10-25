package ch.unil.pafanalysis.common

import ch.unil.pafanalysis.analysis.model.Header

data class ImputationTable(val headers: List<Header>?, val rows: List<List<Boolean?>>?)