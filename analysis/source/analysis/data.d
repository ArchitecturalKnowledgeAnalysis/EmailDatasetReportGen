module analysis.data;

import std.datetime : SysTime;

/** 
 * Represents a single email entry.
 */
class Email {
    long id;
    long parentId;
    string messageId;
    string subject;
    string inReplyTo;
    string sentFrom;
    SysTime date;
    string body;
    string[] tags;

    bool isThreadRoot() {
        return parentId == -1;
    }
}

/** 
 * A set of emails as obtained from a dataset, with some pre-computed arrays
 * for faster data lookup.
 */
class EmailSet {
    Email[] emails;
    Email[] rootEmails;
    Email[long] emailsById;
    Email[][long] repliesById;
    string[] tags;
    Email[][string] emailsByTag;
}

EmailSet parseEmailsJson(string jsonFile) {
    import std.json;
    import std.file;
    import std.array;
    import std.algorithm;

    EmailSet set = new EmailSet;
    RefAppender!(Email[]) emailAppender = appender(&set.emails);
    RefAppender!(string[]) tagAppender = appender(&set.tags);
    RefAppender!(Email[]) rootEmailAppender = appender(&set.rootEmails);
    JSONValue rootArray = parseJSON(readText(jsonFile));
    foreach (JSONValue emailObj; rootArray.array) {
        JSONValue[string] o = emailObj.object;
        Email email = new Email;
        email.id = o["id"].integer;
        email.parentId = o["parent_id"].isNull ? -1 : o["parent_id"].integer;
        email.messageId = o["message_id"].str;
        email.subject = o["subject"].str;
        email.inReplyTo = o["in_reply_to"].isNull ? null : o["in_reply_to"].str;
        email.sentFrom = o["sent_from"].str;
        email.date = SysTime.fromISOExtString(o["date"].str);
        email.body = o["body"].str;
        email.tags = [];
        foreach (JSONValue tagValue; o["tags"].array) {
            string tag = tagValue.str;
            email.tags ~= tag;
            if (!canFind(set.tags, tag)) {
                tagAppender ~= tag;
                set.emailsByTag[tag] = [email];
            } else {
                set.emailsByTag[tag] ~= email;
            }
        }
        set.emailsById[email.id] = email;
        set.repliesById[email.id] = [];
        emailAppender ~= email;
        if (email.parentId == -1) rootEmailAppender ~= email;
    }
    // Second pass over emails to register all replies in the map for fast lookup.
    foreach (ref Email email; set.emails) {
        if (email.parentId != -1) {
            set.repliesById[email.parentId] ~= email;
        }
    }
    return set;
}

struct SearchQueryData {
    string queryName;
    string query;
    long[] threadIds;
    long[] emailIds;
}

SearchQueryData[] parseSearchesJson(string jsonFile) {
    import std.json;
    import std.file;
    import std.array;
    import std.algorithm;

    SearchQueryData[] searches;
    auto searchAppender = appender(&searches);
    JSONValue root = parseJSON(readText(jsonFile));
    foreach (JSONValue searchObj; root.array) {
        JSONValue[string] o = searchObj.object;
        SearchQueryData s;
        s.queryName = o["name"].str;
        s.query = o["query"].str;
        auto threadAppender = appender(&s.threadIds);
        foreach (JSONValue threadIdValue; o["threads"].array) {
            threadAppender ~= threadIdValue.integer;
        }
        auto emailAppender = appender(&s.emailIds);
        foreach (JSONValue emailIdValue; o["emails"].array) {
            emailAppender ~= emailIdValue.integer;
        }
        searchAppender ~= s;
    }
    return searches;
}

bool hasAk(Email email, string[] akTags) {
    import std.algorithm : canFind;
    foreach (tag; email.tags) {
        if (akTags.canFind(tag)) return true;
    }
    return false;
}

bool threadHasAk(Email email, EmailSet set, string[] akTags) {
    return countTagsRecursive(email, set, akTags) > 0;
}

double emailRelevance(Email email, string[] akTags) {
    return hasAk(email, akTags) ? 1.0 : 0.0;
}

double threadRelevance(Email rootEmail, EmailSet set, string[] akTags, double maxRelevance) {
    import std.algorithm : min;
    uint tagCount = countTagsRecursive(rootEmail, set, akTags);
    return min(1.0, tagCount / maxRelevance);
}

double getMaxRelevance(EmailSet set, string[] akTags) {
    import std.algorithm : sort, mean;
    uint[] rootEmailTagCounts = new uint[set.rootEmails.length];
    foreach (i, rootEmail; set.rootEmails) {
        rootEmailTagCounts[i] = countTagsRecursive(rootEmail, set, akTags);
    }
    rootEmailTagCounts.sort!("a > b");
    return rootEmailTagCounts[0 .. 10].mean;
}

uint countTagsRecursive(Email email, EmailSet set, string[] akTags) {
    import std.algorithm;
    uint count = cast(uint) email.tags.count!(t => akTags.canFind(t));
    foreach (Email reply; set.repliesById[email.id]) {
        count += countTagsRecursive(reply, set, akTags);
    }
    return count;
}

uint threadSize(Email email, EmailSet set) {
    uint count = 1;
    foreach (Email reply; set.repliesById[email.id]) {
        count += threadSize(reply, set);
    }
    return count;
}

string[] threadParticipants(Email email, EmailSet set) {
    import std.algorithm : canFind;
    string[] participants = [email.sentFrom];
    foreach (Email reply; set.repliesById[email.id]) {
        foreach (p; threadParticipants(reply, set)) {
            if (!canFind(participants, p)) participants ~= p;
        }
    }
    return participants;
}

string[] threadTags(Email email, EmailSet set) {
    import std.algorithm : canFind;
    string[] tags = email.tags.dup;
    foreach (Email reply; set.repliesById[email.id]) {
        foreach (tag; threadTags(reply, set)) {
            if (!tags.canFind(tag)) tags ~= tag;
        }
    }
    return tags;
}

string[][] generateTagPermutations(uint size, string[] tags) {
    import std.algorithm : canFind;
    import std.array : appender;
    if (size == 0) return [];
    string[][] permutations;
    auto app = appender(&permutations);
    if (size == 1) {
        foreach (tag; tags) {
            app ~= [tag];
        }
    } else {
        string[][] firstPermutations = generateTagPermutations(size - 1, tags);
        foreach (string[] permutation; firstPermutations) {
            foreach (tag; tags) {
                if (!permutation.canFind(tag)) {
                    string[] newPermutation = permutation.dup;
                    newPermutation ~= tag;
                    app ~= newPermutation;
                }
            }
        }
    }
    return permutations;
}