import std.stdio;
import std.file;
import std.json;
import std.datetime;
import analysis.data;
import analysis.base;
import analysis.types;

int main(string[] args) {
	if (args.length < 3) {
		writeln("Missing required arguments. This program requires a path to an \"emails.json\", and \"searches.json\" file.");
		return 1;
	}
	string emailsJson = args[1];
	string searchesJson = args[2];

	SysTime now = Clock.currTime(UTC());
	JSONValue result;
	result["timestamp"] = now.toISOExtString;

	EmailSet set = parseEmailsJson(emailsJson);
	string[] akTags = ["existence", "existence-behavioral", "existence-structural", "property", "process", "technology"];

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

	SearchQueryData[] searches = parseSearchesJson(searchesJson);
	SearchQueryAnalysis[] searchAnalyses;
	searchAnalyses ~= new SearchPrecisionAnalysis();
	foreach (searchData; searches) {
		foreach (analysis; searchAnalyses) {
			analysis.accept(set, searchData, akTags, result);
		}
	}

	if (args.length > 3 && args[3] == "min") {
		writeln(result.toJSON(false));
	} else {
		writeln(result.toJSON(true));
	}

	return 0;
}
