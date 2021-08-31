package org.jetbrains.research.kotlinSlicing

import org.jetbrains.research.pluginUtilities.runners.BaseRunner

object KotlinSlicingRunner : BaseRunner<SlicingRunnerArgs, SlicingRunnerArgsParser>
    ("kotlin-slicing", SlicingRunnerArgsParser) {
    override fun run(args: SlicingRunnerArgs) {
        SlicingExecutor(args.outputDir, args.slice).execute(args.inputDir)
    }
}
