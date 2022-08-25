#!/usr/bin/env dub
/+ dub.sdl:
    dependency "dsh" version="~>1.6.1"
    dependency "requests" version="~>2.0.8"
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
import requests;
import std.algorithm;
import std.path;

int main(string[] args) {
    string[] options = args[1 .. $];

    if (options.canFind("help")) {
        writeln(
            "The run_pipeline script will build and run a set of programs\n" ~
            "to extract, process, and visualize an email dataset.\n" ~
            "  The \"rebuild\" flag can be provided to indicate that all\n" ~
            "    programs should be rebuilt from source before analyzing.\n" ~
            "  The \"clean\" flag can be provided to clean the directory."
        );
        return 0;
    }

    if (canFind(options, "clean")) {
        print("Cleaning files.");
        clean();
        return 0;
    }

    if (
        options.canFind("rebuild") ||
        !exists("bin")
    ) {
        print("Rebuilding dependencies.");
        buildDependencies();
        // If the user explicitly specified to rebuild, exit now.
        if (options.canFind("rebuild")) return 0;
    }

    // Run the pipeline here!
    if (options.length == 0) {
        error("Missing required dataset directory argument.");
        return 1;
    }

    const binDir = buildPath(getcwd(), "bin");

    // Extract data.
    runOrQuit("java -jar " ~ buildPath(binDir, "intake.jar") ~ " " ~ options[0]);
    string reportDir = getLatestReport();
    
    // Analysis.
    print("Running analysis on generated data in %s.", reportDir);
    const analysisResultsFile = buildPath(reportDir, "analysis_results.json");
    new ProcessBuilder()
        .workingDir(reportDir)
        .outputTo(analysisResultsFile)
        .run(buildPath(binDir, "analysis_program") ~ " -e emails.json -s searches.json");

    // Visualization.
    print("Generating visualizations.");
    const visualizationsDir = buildPath(reportDir, "visual");
    mkdir(visualizationsDir);
    new ProcessBuilder()
        .workingDir(visualizationsDir)
        .run("java -jar " ~ buildPath(binDir, "visualizer.jar") ~ " " ~ absolutePath(analysisResultsFile));

    return 0;
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
    reportFiles ~= "bin";
    writefln!"Removing files and directories: %s"(reportFiles);
    removeAnyIfExists(reportFiles);
}

void download(string url, string filePath) {
    Request rq = Request();
    rq.useStreaming = true;
    Response rs = rq.get(url);
    auto stream = rs.receiveAsRange();
    File file = File(filePath, "wb");
    while (!stream.empty) {
        file.rawWrite(stream.front);
        stream.popFront;
    }
    file.close();
}

void buildDependencies() {
    import core.thread;
    const mainDir = getcwd();

    const binDir = buildPath(mainDir, "bin");
    if (!exists(binDir)) mkdir(binDir);

    const libDir = buildPath(mainDir, "lib");
    if (!exists(libDir)) mkdir(libDir);

    // First download any prerequisites.
    // TODO: Support other systems besides Linux X64?
    print("Checking for prerequisite build components...");
    Thread[] prerequisiteThreads;
    version (linux) {
        version (X86_64) {
            const dmdPath = buildPath(libDir, "dmd2", "linux", "bin64", "dmd");
            const dmdBin = buildPath(libDir, "dmd2", "linux", "bin64");
        }
        if (!exists(buildPath(libDir, "dmd2"))) {
            prerequisiteThreads ~= new Thread(() {
                print("Downloading dmd");
                const dmdArchive = buildPath(libDir, "dmd.tar.xz");
                download("https://s3.us-west-2.amazonaws.com/downloads.dlang.org/releases/2022/dmd.2.100.0.linux.tar.xz", dmdArchive);
                runOrQuit("tar -xf " ~ dmdArchive ~ " -C " ~ libDir);
                remove(dmdArchive);
                runOrQuit("chmod +x " ~ dmdPath);
            });
        }

        const jdkName = "jdk-17.0.4.1+1";
        const jdkHome = buildPath(libDir, "jdk-17.0.4.1+1");
        const jdkBin = buildPath(jdkHome, "bin");
        if (!exists(buildPath(libDir, jdkName))) {
            prerequisiteThreads ~= new Thread(() {
                print("Downloading JDK17...");
                const jdkArchive = buildPath(libDir, "openjdk.tar.gz");
                version (X86_64) {
                    const jdkUrl = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.4.1%2B1/OpenJDK17U-jdk_x64_linux_hotspot_17.0.4.1_1.tar.gz";
                }
                download(jdkUrl, jdkArchive);
                runOrQuit("tar -xzf " ~ jdkArchive ~ " -C " ~ libDir);
                remove(jdkArchive);
            });
        }

        const reportGenDir = buildPath(libDir, "report-gen");
        if (!exists(reportGenDir)) {
            prerequisiteThreads ~= new Thread(() {
                print("Downloading analysis repository...");
                const reportGenArchive = buildPath(libDir, "report-gen.zip");
                download("https://github.com/ArchitecturalKnowledgeAnalysis/EmailDatasetReportGen/archive/refs/heads/main.zip", reportGenArchive);
                runOrQuit("unzip " ~ reportGenArchive ~ " -d " ~ reportGenDir);
                remove(reportGenArchive);
                const repoDir = buildPath(reportGenDir, "EmailDatasetReportGen-main");
                copyDir(repoDir, reportGenDir);
                rmdirRecurse(repoDir);
                runOrQuit("chmod +x " ~ buildPath(reportGenDir, "intake", "mvnw"));
                runOrQuit("chmod +x " ~ buildPath(reportGenDir, "visual", "jVisualizer", "mvnw"));
            });
        }
    } else {
        error("Only Linux x86_64 is supported.");
        assert(false);
    }
    foreach (t; prerequisiteThreads) t.start();
    foreach (t; prerequisiteThreads) t.join();
    
    // Use our own downloaded binaries for the builds.
    const pipelinePath = jdkBin ~ pathSeparator ~ dmdBin ~ pathSeparator ~ getEnv("PATH");

    Thread t1 = new Thread(() {
        print("Building intake.jar");
        const intakeDir = buildPath(reportGenDir, "intake");
        const logFile = buildPath(binDir, "intake_build.log");
        int result = new ProcessBuilder()
            .workingDir(intakeDir)
            .outputTo(logFile)
            .errorTo(logFile)
            .withEnv("JAVA_HOME", jdkHome)
            .withEnv("PATH", pipelinePath)
            .run("./mvnw -B clean package -DskipTests=true");
        if (result != 0) throw new Exception("Intake build failed.");
        string jarFile = findFile(buildPath(intakeDir, "target"), ".*-jar-with-dependencies\\.jar", false);
        copy(jarFile, buildPath(binDir, "intake.jar"));
    });
    Thread t2 = new Thread(() {
        print("Building visualizer.jar");
        const visualizerDir = buildPath(reportGenDir, "visual", "jVisualizer");
        const logFile = buildPath(binDir, "visualizer_build.log");
        int result = new ProcessBuilder()
            .workingDir(visualizerDir)
            .outputTo(logFile)
            .errorTo(logFile)
            .withEnv("JAVA_HOME", jdkHome)
            .withEnv("PATH", pipelinePath)
            .run("./mvnw -B clean package -DskipTests=true");
        if (result != 0) throw new Exception("Visualizer build failed.");
        string jarFile = findFile(buildPath(visualizerDir, "target"), ".*-jar-with-dependencies\\.jar", false);
        copy(jarFile, buildPath(binDir, "visualizer.jar"));
    });
    Thread t3 = new Thread(() {
        print("Building analysis_program");
        const analysisDir = buildPath(reportGenDir, "analysis");
        const logFile = buildPath(binDir, "analysis_program_build.log");
        int result = new ProcessBuilder()
            .workingDir(analysisDir)
            .outputTo(logFile)
            .errorTo(logFile)
            .run("dub build --build=release --force --compiler=" ~ dmdPath);
        if (result != 0) throw new Exception("analysis_program build failed.");
        copy(buildPath(analysisDir, "analysis"), buildPath(binDir, "analysis_program"));
        version(linux) {
            result = run("chmod +x " ~ buildPath(binDir, "analysis_program"));
            if (result != 0) throw new Exception("Could not set the analysis_program as executable.");
        }
    });
    // Run all three builds in parallel!
    t1.start();
    t2.start();
    t3.start();

    t1.join();
    t2.join();
    t3.join();
    print("Done.");
}
