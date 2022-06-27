module analysis.types.count;

import std.json;
import analysis.base;
import analysis.data;

class CountAnalysis : Analysis {
    private uint akEmailCount;
    private uint akThreadCount;
    private ulong emailCount;
    private ulong threadCount;
    private uint[string] emailTagCounts;
    private uint[string] threadTagCounts;

    void initialize(EmailSet set, string[] akTags) {
        emailCount = set.emails.length;
        threadCount = set.rootEmails.length;
        foreach (tag; akTags) {
            emailTagCounts[tag] = 0;
            threadTagCounts[tag] = 0;
        }
    }

    void accept(Email email, EmailSet set, string[] akTags) {
        foreach (tag; email.tags) {
            emailTagCounts[tag]++;
        }
        if (hasAk(email, akTags)) akEmailCount++;
        if (email.isThreadRoot) {
            foreach (tag; threadTags(email, set)) {
                threadTagCounts[tag]++;
            }
            if (threadHasAk(email, set, akTags)) akThreadCount++;
        }
    }

    void addToJson(ref JSONValue obj) {
        JSONValue o;
        o["total_emails"] = emailCount;
        o["total_threads"] = threadCount;
        o["total_ak_emails"] = akEmailCount;
        o["total_ak_threads"] = akThreadCount;
        o["email_tag_counts"] = emailTagCounts;
        o["thread_tag_counts"] = threadTagCounts;
        obj["count"] = o;
    }
}