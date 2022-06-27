#!/usr/bin/env dub
/+ dub.sdl:
    dependency "dsh" version="~>1.6.1"
+/

/**
 * This script will remove any previously generated reports.
 */
module clean_reports;

import dsh;
import std.algorithm.searching : startsWith;

void main() {
    string[] reportFiles;
    walkDir(".", (entry) {
        if (startsWith(entry.name, "./report")) {
            reportFiles ~= entry.name;
        }
    }, false);
    writefln!"Will remove the following files and/or directories: %s"(reportFiles);
    removeAnyIfExists(reportFiles);
}

