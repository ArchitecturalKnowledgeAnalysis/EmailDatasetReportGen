module analysis.types.search_precision;

import std.algorithm;
import std.array;
import std.json;
import std.stdio;
import analysis.data;

/** 
 * Computes the precision for a single search query.
 */
class SearchPrecisionAnalysis : SearchQueryAnalysis {
    private double[] emailPrecision;
    private double[] emailNdcg;
    private double[] threadPrecision;
    private double[] threadNdcg;
    private double threadMaxRelevance;

    private double[][string] emailTagPrecisions;
    private double[][string] emailTagNdcg;
    private double[][string] threadTagPrecisions;
    private double[][string] threadTagNdcg;
    private double[string] threadTagMaxRelevance;

    void accept(EmailSet set, SearchQueryData data, string[] akTags, ref JSONValue obj) {
        JSONValue precisionData;
        precisionData["query_name"] = data.queryName;
        precisionData["query"] = data.query;
        precisionData["all_tags"] = runAnalysis(data, set, akTags);
        foreach (tag; akTags) {
            precisionData[tag] = runAnalysis(data, set, [tag]);
        }
        obj["search_precision_" ~ data.queryName] = precisionData;
    }

    private JSONValue runAnalysis(SearchQueryData data, EmailSet set, string[] akTags) {
        threadMaxRelevance = getMaxRelevance(set, akTags);
        double[] emailRelevances = getEmailRelevances(data.emailIds, set, akTags);
        double[] idealEmailRelevances = getIdealEmailRelevances(set, akTags);
        emailNdcg = iterativeNdcg(emailRelevances, idealEmailRelevances);
        emailPrecision = iterativePrecision(emailRelevances);

        double[] threadRelevances = getThreadRelevances(data.threadIds, set, akTags, threadMaxRelevance);
        double[] idealThreadRelevances = getIdealThreadRelevances(set, akTags, threadMaxRelevance);
        threadNdcg = iterativeNdcg(threadRelevances, idealThreadRelevances);
        threadPrecision = iterativePrecision(threadRelevances);

        JSONValue j;
        j["tags"] = akTags;
        j["thread_max_relevance"] = threadMaxRelevance;
        j["email_precision"] = emailPrecision;
        j["email_ndcg"] = emailNdcg;
        j["thread_precision"] = threadPrecision;
        j["thread_ndcg"] = threadNdcg;
        return j;
    }

    private static double[] getEmailRelevances(long[] ids, EmailSet set, string[] akTags) {
        return ids.map!((id) {
            if (id in set.emailsById) {
                return emailRelevance(set.emailsById[id], akTags);
            }
            return 0.0;
        }).array;
    }

    private static double[] getThreadRelevances(long[] ids, EmailSet set, string[] akTags, double maxRelevance) {
        return ids.map!((id) {
            if (id in set.emailsById) {
                return threadRelevance(set.emailsById[id], set, akTags, maxRelevance);
            }
            return 0.0;
        }).array;
    }

    private static double[] getIdealEmailRelevances(EmailSet set, string[] akTags) {
        double[] relevances = set.emails
            .map!(email => emailRelevance(email, akTags))
            .array;
        relevances.sort!((a, b) => a > b);
        return relevances;
    }

    private static double[] getIdealThreadRelevances(EmailSet set, string[] akTags, double maxRelevance) {
        double[] relevances = set.rootEmails
            .map!(email => threadRelevance(email, set, akTags, maxRelevance))
            .array;
        relevances.sort!((a, b) => a > b);
        return relevances;
    }

    private static double dcg(double[] relevances) {
        import std.math : log2;
        double sum = 0;
        for (int i = 1; i <= relevances.length; i++) {
            double relI = relevances[i - 1];
            sum += (relI / log2(i + 1));
        }
        return sum;
    }

    private static double ndcg(double[] relevances, double[] idealRelevances) {
        import std.algorithm : min;
        size_t size = min(relevances.length, idealRelevances.length);
        if (size == 0) return 0;
        double realGain = dcg(relevances[0 .. size]);
        double idealGain = dcg(idealRelevances[0 .. size]);
        return realGain / idealGain;
    }

    private static double[] iterativeNdcg(double[] relevances, double[] idealRelevances) {
        import std.algorithm : min;
        size_t size = min(relevances.length, idealRelevances.length);
        double[] values = new double[size];
        for (size_t i = 0; i < size; i++) {
            size_t n = i + 1;
            values[i] = ndcg(relevances[0 .. n], idealRelevances[0 .. n]);
        }
        return values;
    }

    private static double[] iterativePrecision(double[] relevances) {
        import std.algorithm;
        double[] values = new double[relevances.length];
        foreach (i, relevance; relevances) {
            values[i] = relevances[0 .. i + 1]
                .map!(rel => rel > 0 ? 1.0 : 0.0)
                .mean;
        }
        return values;
    }
}