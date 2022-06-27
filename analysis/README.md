# Architectural Knowledge Analysis Suite
This program contains a suite of analyses that can be run on a dataset exported using the Java tool from the `../intake` directory.

The program can be built and run using the `dub` package manager. Run `dub build` to build an executable.

When running the program, provide two command-line arguments for the paths to the `emails.json` and `searches.json` that are produced by the Java tool. These arguments are required.

For example:
```
dub build
./analysis ../intake/report_1/emails.json ../intake/report_1/searches.json
```