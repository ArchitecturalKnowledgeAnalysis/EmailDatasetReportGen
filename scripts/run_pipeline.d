#!/usr/bin/env dub
/+ dub.sdl:
    dependency "dsh" version="~>1.6.1"
+/
import dsh;
import std.algorithm;

const DATASET_DIR = "/home/andrew/Programming/ArchitecturalKnowledgeAnalysis/Thesis/datasets/current";
// const REPORT_DEST_DIR = "/home/andrew/Programming/ArchitecturalKnowledgeAnalysis/EmailSearchEfficacyThesis/report";
const REPORT_DEST_DIR = "/home/andrew/Programming/ArchitecturalKnowledgeAnalysis/EmailDatasetReportGen/scripts/report";
const ANALYSIS_OUTPUT = "analysis_results.json";

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

    if (canFind(options, "rebuild") || !exists("intake.jar")) {
        chdir("../intake");
        runOrQuit("mvn clean package");
        chdir(currentWorkingDir);
        string jarFile = findFile("../intake/target", ".*-jar-with-dependencies\\.jar", false);
        copy(jarFile, "intake.jar");
    }
    if (canFind(options, "rebuild") || !exists("analysis")) {
        chdir("../analysis");
        runOrQuit("dub build --build=release --force");
        chdir(currentWorkingDir);
        copy("../analysis/analysis", "analysis");
        runOrQuit("chmod +x analysis");
    }
    if (canFind(options, "rebuild")) return; // If the user indicated rebuilding, don't run analysis.
    
    runOrQuit("java -jar intake.jar " ~ DATASET_DIR);
    string reportDir = getLatestReport();
    print("Running analysis on generated data in %s.", reportDir);
    auto pb = new ProcessBuilder()
        .workingDir(reportDir)
        .outputTo(reportDir ~ "/" ~ ANALYSIS_OUTPUT);
    pb.run("../analysis emails.json searches.json min");
    pb.outputTo(reportDir ~ "/analysis_results_pretty.json");
    pb.run("../analysis emails.json searches.json");

    // TODO: Visualize data.

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
    reportFiles ~= ANALYSIS_OUTPUT;
    reportFiles ~= ".dub";
    reportFiles ~= "intake.jar";
    reportFiles ~= "analysis";
    writefln!"Removing files and directories: %s"(reportFiles);
    removeAnyIfExists(reportFiles);
}
