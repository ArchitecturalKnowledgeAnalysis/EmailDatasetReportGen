# EmailDatasetReportGen
Utility for deep analysis of email datasets and generation of reports in various formats.

The main Java program is responsible for processing a dataset whose directory is passed to it via a single command-line argument.

There's also a set of toolchain scripts, written in D, that can be used to automate the flow of generating data.
- `clean_reports.d` - Removes any `report_` directory or file in the working directory.
- `export_data.d` - Copies the contents of the latest report to a target directory. Useful for when you are using the data in some other report or document that uses relative paths to resources.
- `run_pipeline.d` - Compiles and runs the report generator to generate a report for a dataset, and exports this to a configured location via the `export_data` script.
> These scripts make use of [DSH](https://code.dlang.org/packages/dsh). It's advised that you have installed `dshutil` in order to run the pipeline.