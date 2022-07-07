import std.stdio;
import std.file;
import std.json;
import std.datetime;
import std.typecons;
import args;
import analysis.data;
import analysis.types;

int main(string[] args) {
	auto nOpt = getOptions(args);
	if (nOpt.isNull) return 0;
	AnalysisOptions options = nOpt.get();

	SysTime now = Clock.currTime(UTC());
	JSONValue result;
	result["timestamp"] = now.toISOExtString;

	string[] akTags = ["existence", "existence-behavioral", "existence-structural", "property", "process", "technology"];

	EmailSet set = doEmailAnalyses(options.emailsJsonFilename, akTags, result);
	doSearchAnalyses(options.searchesJsonFilename, akTags, result, set);

	if (options.minifyJson) {
		writeln(result.toJSON(false));
	} else {
		writeln(result.toJSON(true));
	}

	return 0;
}

EmailSet doEmailAnalyses(string emailsJsonFilename, string[] akTags, ref JSONValue result) {
	EmailSet set = parseEmailsJson(emailsJsonFilename);
	Analysis[] analyses;
	analyses ~= new CharacteristicAnalysis();
	analyses ~= new CountAnalysis();
	analyses ~= new RelevanceAnalysis();
	analyses ~= new CoOccurrenceAnalysis(2);
	analyses ~= new CoOccurrenceAnalysis(3);
	analyses ~= new NGramAnalysis(2, false);
	analyses ~= new NGramAnalysis(2, true);
	analyses ~= new NGramAnalysis(3, false);
	analyses ~= new NGramAnalysis(3, true);
	foreach (analysis; analyses) analysis.initialize(set, akTags);
	foreach (email; set.emails) {
		foreach (analysis; analyses) analysis.accept(email, set, akTags);
	}
	foreach (analysis; analyses) analysis.addToJson(result);
	return set;
}

void doSearchAnalyses(string searchesJsonFilename, string[] akTags, ref JSONValue result, EmailSet set) {
	SearchQueryData[] searches = parseSearchesJson(searchesJsonFilename);
	SearchQueryAnalysis[] searchAnalyses;
	searchAnalyses ~= new SearchPrecisionAnalysis();
	foreach (searchData; searches) {
		foreach (analysis; searchAnalyses) {
			analysis.accept(set, searchData, akTags, result);
		}
	}
}

// TODO: Make my own options parser.

struct AnalysisOptions {
	@Arg("The JSON file containing emails.", 'e', Optional.no)
	string emailsJsonFilename;
	@Arg("The JSON file containing search data.", 's', Optional.no)
	string searchesJsonFilename;
	@Arg("Whether to output minified JSON.", Optional.yes)
	bool minifyJson;
}

/** 
 * Gets options from the command-line, if possible.
 * Params:
 *   args = The command-line arguments.
 * Returns: The options, which might be null.
 */
Nullable!AnalysisOptions getOptions(string[] args) {
	AnalysisOptions options;
	Nullable!AnalysisOptions nOpt;
	try {
		bool helpWanted = parseArgsWithConfigFile(options, args);
		if (helpWanted) {
			printArgsHelp(options, "Performs analysis over exported email dataset data.");
		} else {
			nOpt = options;
		}
	} catch (Exception e) {
		printArgsHelp(options, cast(string) e.message);
	}
	return nOpt;
}
