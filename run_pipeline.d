#!/usr/bin/env dub
/+ dub.sdl:
    dependency "dsh" version="~>1.6.1"
+/
import dsh;
import std.algorithm;

const DATASET_DIR = "/home/andrew/Programming/ArchitecturalKnowledgeAnalysis/Thesis/datasets/current";
const REPORT_DEST_DIR = "/home/andrew/Programming/ArchitecturalKnowledgeAnalysis/EmailSearchEfficacyThesis/report";

void main(string[] args) {
    string[] options = args[1 .. $];
    if (!canFind(options, "no-build")) {
        runOrQuit("mvn clean package");
    }
    string jarFile = findFile("target", ".*-jar-with-dependencies\\.jar", false);
    copy(jarFile, "report-gen.jar");
    runOrQuit("java -jar report-gen.jar " ~ DATASET_DIR);
    std.file.remove("report-gen.jar");
    if (!exists("export_data")) {
        runOrQuit("dshutil compile export_data.d");
    }
    runOrQuit("./export_data " ~ REPORT_DEST_DIR);
    if (canFind(options, "clean")) {
        if (!exists("clean_reports")) {
            runOrQuit("dshutil compile clean_reports.d");
        }
        runOrQuit("./clean_reports");
    }
}

