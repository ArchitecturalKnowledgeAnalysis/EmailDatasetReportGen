#!/usr/bin/env dub
/+ dub.sdl:
    dependency "dsh" version="~>1.6.1"
+/
import dsh;
import std.algorithm;

int main(string[] args) {
    string DEST_DIR = args[1];
    string[] reportFiles;
    walkDir(".", (entry) {
        if (startsWith(entry.name, "./report") && entry.isDir) {
            reportFiles ~= entry.name;
        }
    }, false);
    reportFiles.sort();
    if (reportFiles.length < 1) {
        error("Missing report.");
        return 1;
    }
    string latestReportDir = reportFiles[$ - 1];
    removeIfExists(DEST_DIR);
    print("Copying %s to %s", latestReportDir, DEST_DIR);
    copyDir(latestReportDir, DEST_DIR);

    return 0;
}

