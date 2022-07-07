#!/usr/bin/env dub
/+ dub.sdl:
    dependency "dsh" version="~>1.6.1"
+/

/**
 * This script is responsible for running all the steps of analysis for a given
 * email dataset. It manages building necessary dependencies from the other
 * project directories in this repository, and calling them in sequence to
 * first extract data, perform analysis, and visualize the results, and finally
 * export the data to a specified directory.
 */
module run_pipeline;

import dsh;
import std.algorithm;

const DATASET_DIR = "/home/andrew/Programming/ArchitecturalKnowledgeAnalysis/Thesis/datasets/current";
// const REPORT_DEST_DIR = "/home/andrew/Programming/ArchitecturalKnowledgeAnalysis/EmailSearchEfficacyThesis/report";
const REPORT_DEST_DIR = "/home/andrew/Programming/ArchitecturalKnowledgeAnalysis/EmailDatasetReportGen/scripts/report";

void main(string[] args) {
    string[] options = args[1 .. $];

    if (options.canFind("help")) {
        writeln(
            "The run_pipeline script will build and run a set of programs\n" ~
            "to extract, process, and visualize an email dataset.\n" ~
            "  The \"rebuild\" flag can be provided to indicate that all\n" ~
            "    programs should be rebuilt from source before analyzing.\n" ~
            "  The \"clean\" flag can be provided to clean the directory."
        );
        return;
    }

    string currentWorkingDir = getcwd;

    if (canFind(options, "clean")) {
        print("Cleaning files.");
        clean();
        return;
    }

    // Build dependencies, if needed.
    if (canFind(options, "rebuild") || !exists("intake.jar")) {
        chdir("../intake");
        runOrQuit("mvn clean package");
        chdir(currentWorkingDir);
        string jarFile = findFile("../intake/target", ".*-jar-with-dependencies\\.jar", false);
        copy(jarFile, "intake.jar");
    }
    if (canFind(options, "rebuild") || !exists("visualizer.jar")) {
        chdir("../visual/jVisualizer");
        runOrQuit("mvn clean package");
        chdir(currentWorkingDir);
        string jarFile = findFile("../visual/jVisualizer/target", ".*-jar-with-dependencies\\.jar", false);
        copy(jarFile, "visualizer.jar");
    }
    if (canFind(options, "rebuild") || !exists("analysis")) {
        chdir("../analysis");
        runOrQuit("dub build --build=release --force");
        chdir(currentWorkingDir);
        copy("../analysis/analysis", "analysis");
        runOrQuit("chmod +x analysis");
    }
    if (canFind(options, "rebuild")) return; // If the user indicated rebuilding, don't run analysis.
    
    // Run the pipeline here!

    // Extract data.
    runOrQuit("java -jar intake.jar " ~ DATASET_DIR);
    string reportDir = getLatestReport();
    
    // Analysis.
    print("Running analysis on generated data in %s.", reportDir);
    new ProcessBuilder()
        .workingDir(reportDir)
        .outputTo(reportDir ~ "/analysis_results.json")
        .run("../analysis -e emails.json -s searches.json --minifyJson");
    print("Analysis complete.");

    // Visualization.
    print("Generating visualizations.");
    mkdir(reportDir ~ "/visual");
    new ProcessBuilder()
        .workingDir(reportDir ~ "/visual")
        .run("java -jar ../../visualizer.jar ../analysis_results.json");
    print("Visualization complete.");

    // Export to the destination.
    copyDir(reportDir, REPORT_DEST_DIR);
}

string getLatestReport() {
    string[] reportFiles;
    walkDir(".", (entry) {
        if (startsWith(entry.name, "./report_") && entry.isDir) {
            reportFiles ~= entry.name;
        }
    }, false);
    reportFiles.sort();
    if (reportFiles.length < 1) {
        error("Missing report.");
        return null;
    }
    return reportFiles[$ - 1];
}

void clean() {
    string[] reportFiles;
    walkDir(".", (entry) {
        if (startsWith(entry.name, "./report_")) {
            reportFiles ~= entry.name;
        }
    }, false);
    reportFiles ~= ".dub";
    writefln!"Removing files and directories: %s"(reportFiles);
    removeAnyIfExists(reportFiles);
}
