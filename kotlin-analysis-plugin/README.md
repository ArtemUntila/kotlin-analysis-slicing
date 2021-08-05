# Kotlin-Analysis plugin

The plugin to run project analysis from the CLI:
* ```kotlin-slicing``` - command process slicing

### Usage

To run analysis execute task 'cli' with analysis runner name and args:

```bash
gradle :kotlin-analysis-plugin:cli -Prunner="kotlin-slicing" -Pinput="path/to/dir/with/projects" -Poutput="path/to/dir/with/results" -Pslice="path/to/slice.log"
```

Origin: https://github.com/JetBrains-Research/Kotlin-Analysis/tree/main/kotlin-analysis-plugin
