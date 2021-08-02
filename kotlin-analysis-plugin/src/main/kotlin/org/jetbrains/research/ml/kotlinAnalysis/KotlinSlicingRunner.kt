package org.jetbrains.research.ml.kotlinAnalysis

import org.jetbrains.research.pluginUtilities.runners.BaseRunner
import org.jetbrains.research.pluginUtilities.runners.IORunnerArgs
import org.jetbrains.research.pluginUtilities.runners.IORunnerArgsParser

object KotlinSlicingRunner : BaseRunner<IORunnerArgs, IORunnerArgsParser>
    ("kotlin-slicing", IORunnerArgsParser) {
    override fun run(args: IORunnerArgs) {
        Slicer(args.outputDir).execute(args.inputDir)
    }
}
