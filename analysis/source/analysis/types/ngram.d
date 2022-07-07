module analysis.types.ngram;

import std.json;
import analysis.data;

const string KEY_DELIM = "__";

class NGramAnalysis : Analysis {
    private ubyte size;
    private bool skip;
    private string[][] patterns;
    private long[][][string] emailIdSequences;

    this(ubyte size, bool skip) {
        this.size = size;
        this.skip = skip;
    }

    void initialize(EmailSet set, string[] akTags) {
        patterns = generateTagPermutations(size, akTags);
    }

    void accept(Email email, EmailSet set, string[] akTags) {
        foreach (tag; email.tags) {
            foreach (pattern; patterns) {
                if (pattern.length > 0 && pattern[0] == tag) {
                    long[][] matchingSequences = findMatchingSequences(email, set, pattern);
                    emailIdSequences[patternKey(pattern)] ~= matchingSequences;
                }
            }
        }
    }

    void addToJson(ref JSONValue obj) {
        import std.conv : to;
        import std.array : split;
        JSONValue o;
        foreach (key, idSequences; emailIdSequences) {
            JSONValue patternData;
            patternData["count"] = idSequences.length;
            patternData["pattern"] = key.split(KEY_DELIM);
            patternData["sequences"] = idSequences;
            o[key] = patternData;
        }
        string name = "ngram_" ~ size.to!string ~ (skip ? "_skip" : "_no-skip");
        obj[name] = o;
    }

    private string patternKey(string[] p) {
        import std.array : join;
        return join(p, KEY_DELIM);
    }

    private long[][] findMatchingSequences(Email email, EmailSet set, string[] pattern) {
        import std.algorithm;
        import std.array;
        if (pattern.length == 0) return [];
        bool thisEmailMatches = email.tags.canFind(pattern[0]);
        long[][] sequences = [];
        auto sequenceAppender = appender(&sequences);
        if (thisEmailMatches) {
            if (pattern.length == 1) {
                sequenceAppender ~= [email.id];
            } else {
                foreach (reply; set.repliesById[email.id]) {
                    foreach (sequence; findMatchingSequences(reply, set, pattern[1 .. $])) {
                        sequence.insertInPlace(0, email.id);
                        sequenceAppender ~= sequence;
                    }
                }
            }
        } else if (skip) {
            foreach (reply; set.repliesById[email.id]) {
                sequenceAppender ~= findMatchingSequences(reply, set, pattern);
            }
        }
        return sequences;
    }
}