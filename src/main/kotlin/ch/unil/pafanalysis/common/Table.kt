package ch.unil.pafanalysis.common

import ch.unil.pafanalysis.analysis.model.Header

data class Table(val headers: List<Header>?, val rows: List<List<Any>>?)

