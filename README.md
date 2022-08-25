# EmailDatasetReportGen
Utility for deep analysis of email datasets and generation of reports in various formats.

It contains the following components:

- In `intake`, a Java program is written which extracts all information from an email dataset into an `emails.json` and a `searches.json` which can be used for further analysis.
- In `analysis`, a D program is written which takes the JSON data produced by the intake program, and outputs JSON containing the analysis data.
- In `visual`, a Java program takes the analysis data and generates a series of graphics for it.
- `run_pipeline.d` is the main script which runs all of the the aforementioned steps in sequence to extract, analyze, and visualize the data.

> These scripts make use of [DSH](https://code.dlang.org/packages/dsh). It's advised that you have installed `dshutil` in order to run the pipeline.

## Running the Analysis
> Requirements:
> - Linux Operating System
> - x86_64 CPU Architecture
> - Have the following programs installed: `tar`, `chmod`, `unzip`

To run the analysis, first download the latest `report-gen` binary executable from the [releases](https://github.com/ArchitecturalKnowledgeAnalysis/EmailDatasetReportGen/releases) page, or clone this repository and compile `run_pipeline.d` using your installed D toolchain.

Then, simply run the executable, and provide as a single argument the path to an email dataset directory. For example,
```sh
./report-gen /home/andrew/Downloads/iteration-9
```
This will generate a new report, in the directory where this program was invoked.

## The Pipeline
This program runs a series of steps to perform analysis in a consistent, efficient manner. Here's a simple overview of the logical flow of the analysis pipeline.

1. Download any missing third-party components (like DMD compiler and/or JDK17).
2. Download and build the various programs from this repository.
3. Use the `intake` program to read the specified dataset and extract raw information.
4. Use the `analysis` program to process the raw data and generate aggregate statistics and analyses.
5. Use the `visual` program to generate visualizations from the analysis produced by the previous step.
