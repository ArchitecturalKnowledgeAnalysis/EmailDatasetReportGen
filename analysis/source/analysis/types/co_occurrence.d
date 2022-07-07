module analysis.types.co_occurrence;

import std.json;
import analysis.data;

const string KEY_DELIM = "__";

/** 
 * Looks recurring patterns where mutiple tags tend to appear together.
 */
class CoOccurrenceAnalysis : Analysis {
    private string[][] patterns;
    private long[][string] resultMap;
    private ubyte size;

    this(ubyte size) {
        this.size = size;
    }

    void initialize(EmailSet set, string[] akTags) {
        string[][] allPatterns = generateTagPermutations(size, akTags);
        // Ensure that all patterns are unique, since we treat them as sets.
        foreach (p; allPatterns) {
            bool exists = false;
            foreach (p2; patterns) {
                if (patternsEqual(p, p2)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) patterns ~= p;
        }
    }

    void accept(Email email, EmailSet set, string[] akTags) {
        import std.algorithm : all, canFind;
        foreach (pattern; patterns) {
            if (pattern.all!(t => email.tags.canFind(t))) {
                resultMap[patternKey(pattern)] ~= email.id;
            }
        }
    }

    void addToJson(ref JSONValue obj) {
        import std.conv : to;
        import std.array : split;
        JSONValue o;
        foreach (key, emailIds; resultMap) {
            JSONValue patternData;
            patternData["count"] = emailIds.length;
            patternData["pattern"] = key.split(KEY_DELIM);
            patternData["email_ids"] = emailIds;
            o[key] = patternData;
        }
        obj["co-occurrence_" ~ size.to!string] = o;
    }

    private string patternKey(string[] p) {
        import std.array : join;
        return join(p, KEY_DELIM);
    }

    private bool patternsEqual(string[] p1, string[] p2) {
        import std.algorithm : all, canFind;
        return p1.all!(t => p2.canFind(t)) && p2.all!(t => p1.canFind(t));
    }
}