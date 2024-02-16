package ch.unil.pafanalysis.analysis.hints

import ch.unil.pafanalysis.analysis.model.StepHint

class DefaultStepHints {

    val defaultList = listOf<StepHint>(
        StepHint("load-dataset", "Load dataset", "Add description of experiment (for example experiment hypothesis from LIMS). It is easy to forget it otherwise."),
        StepHint("edit-groups", "Edit groups", "Assign groups. It should be automatically done with Spectronaut data. Choose variable to work on : iBAQ or LFQ or Quantity."),
        StepHint("remove-columns", "Columns - Remove columns", "Good to clean up dataset and make them smaller. A default choice is presented. Headers can be renamed if needed."),
        StepHint("filter-rows", "Filter rows: OIBS,  Reverse , contaminants , organism...", "Depending on dataset; sometimes CON need to be kept (ex GFP) in the first exploration steps. Display table to find out."),
        StepHint("transform-log2", "Transform - log2", "Tip: possible also transform total IBAQ, tot intensity. Add \"log2\" to headers."),
        StepHint("boxplot-and-stats", "Boxplot and Stats table", ""),
        StepHint("normalize", "Normalize by median subtraction", "Should not be necessary for MaxQuant LFQ or Spectronaut Quantity, as they are already normalized."),
        StepHint("repeat-boxplot", "Optional: repeat boxplot", "Additional filter: min. number of peptides/precursors (usually 2)."),
        StepHint("filter-on-valid", "Filter on valid", "Filter for min values in at least one condition"),
        StepHint("impute", "Impute missing values", ""),
        StepHint("pca", "PCA", ""),
        StepHint("t-test", "T-test", ""),
        StepHint("volcano-plots", "Volcano plots", ""),
        StepHint("remove-imputed", "Remove imputed values", ""),
        StepHint("enrichment", "1D annotation enrichment", ""),
        StepHint("final-formatting", "Final formatting", "Rename columns, add groups to values columns, see if necessary to reorder columns."),
        StepHint("add-comments", "Add comments everywhere", ""),
        StepHint("export", "Export report or zip file and lock analysis", ""),
        StepHint("send", "Send"),
        StepHint("copy-server", "Make copies of .zip on server", ""),
        StepHint("lims", "Put report on LIMS", "")
        )
}