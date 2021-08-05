package org.jetbrains.research.ml.kotlinAnalysis

import org.jetbrains.research.pluginUtilities.runners.BaseRunner

object KotlinSlicingRunner : BaseRunner<SlicingRunnerArgs, SlicingRunnerArgsParser>
    ("kotlin-slicing", SlicingRunnerArgsParser) {
    override fun run(args: SlicingRunnerArgs) {
        Slicer(args.outputDir, args.slice).execute(args.inputDir)
    }
}
