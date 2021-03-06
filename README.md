# EmailDatasetReportGen
Utility for deep analysis of email datasets and generation of reports in various formats.

It contains the following components:

- In `intake`, a Java program is written which extracts all information from an email dataset into an `emails.json` and a `searches.json` which can be used for further analysis.
- In `analysis`, a D program is written which takes the JSON data produced by the intake program, and outputs JSON containing the analysis data.
- In `visual`, a Java program takes the analysis data and generates a series of graphics for it.
- `run_pipeline.d` is the main script which runs all of the the aforementioned steps in sequence to extract, analyze, and visualize the data.

> These scripts make use of [DSH](https://code.dlang.org/packages/dsh). It's advised that you have installed `dshutil` in order to run the pipeline.
