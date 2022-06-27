module analysis.types.relevance;

import std.json;
import analysis.data;
import analysis.base;

class RelevanceAnalysis : Analysis {
    private double[] emailRelevances;
    private double[] threadRelevances;
    private double[][string] emailTagRelevances;
    private double[][string] threadTagRelevances;

    private double threadMaxRelevance;
    private double[string] threadTagMaxRelevances;

    void initialize(EmailSet set, string[] akTags) {
        threadMaxRelevance = getMaxRelevance(set, akTags);
        foreach (tag; akTags) {
            threadTagMaxRelevances[tag] = getMaxRelevance(set, [tag]);
        }
    }

    void accept(Email email, EmailSet set, string[] akTags) {
        emailRelevances ~= emailRelevance(email, akTags);
        foreach (tag; akTags) {
            emailTagRelevances[tag] ~= emailRelevance(email, [tag]);
        }
        if (email.isThreadRoot) {
            threadRelevances ~= threadRelevance(email, set, akTags, threadMaxRelevance);
            foreach (tag; akTags) {
                threadTagRelevances[tag] ~= threadRelevance(email, set, [tag], threadTagMaxRelevances[tag]);
            }
        }
    }

    void addToJson(ref JSONValue obj) {
        JSONValue o;
        o["max_relevance_all_tags"] = threadMaxRelevance;
        o["max_tag_relevances"] = threadTagMaxRelevances;
        o["email_relevances"] = emailRelevances;
        o["thread_relevances"] = threadRelevances;
        o["email_tag_relevances"] = emailTagRelevances;
        o["thread_tag_relevances"] = threadTagRelevances;
        obj["relevance"] = o;
    }
}