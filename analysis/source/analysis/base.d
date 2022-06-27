module analysis.base;

import std.json;
import analysis.data;

/** 
 * Represents an analysis operation that consumes emails to add to its data,
 * and produces at the end some data which can be added to a JSON object.
 */
interface Analysis {
    void initialize(EmailSet set, string[] akTags);
    void accept(Email email, EmailSet set, string[] akTags);
    void addToJson(ref JSONValue obj);
}

/** 
 * Represents an analysis operation to be performed on dataset search results.
 */
interface SearchQueryAnalysis {
    void accept(EmailSet set, SearchQueryData data, string[] akTags, ref JSONValue obj);
}