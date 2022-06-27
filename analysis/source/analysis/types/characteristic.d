module analysis.types.characteristic;

import std.regex;
import std.json;
import analysis.data;
import analysis.base;

/** 
 * Data that keeps a series of values for a few categories: "any tag", "not ak"
 * and a category for any tag.
 */
struct FrequencyData {
    uint[] anyTag;
    uint[] notAk;
    uint[][string] tagData;

    JSONValue toJson() {
        JSONValue obj;
        obj["any_tag"] = anyTag;
        obj["not_ak"] = notAk;
        obj["tag_data"] = tagData;
        return obj;
    }
}

/** 
 * Gathers some data about the characteristics of emails and/or threads that
 * fall into certain categories.
 */
class CharacteristicAnalysis : Analysis {
    private FrequencyData bodySizeData;
    private FrequencyData wordCountData;
    private FrequencyData threadSizeData;
    private FrequencyData threadParticipationData;

    void initialize(EmailSet set, string[] akTags) {} // No initialization needed.

    void accept(Email email, EmailSet set, string[] akTags) {
        uint bodySize = cast(uint) email.body.length;
        uint wordCount = cast(uint) split(email.body, regex(`\s+`)).length;
        if (hasAk(email, akTags)) {
            bodySizeData.anyTag ~= bodySize;
            wordCountData.anyTag ~= wordCount;
            foreach (tag; akTags) {
                if (hasAk(email, [tag])) {
                    bodySizeData.tagData[tag] ~= bodySize;
                    wordCountData.tagData[tag] ~= wordCount;
                }
            }
        } else {
            bodySizeData.notAk ~= bodySize;
            wordCountData.notAk ~= wordCount;
        }

        if (email.isThreadRoot) {
            uint threadSize = threadSize(email, set);
            uint participantCount = cast(uint) threadParticipants(email, set).length;
            if (threadHasAk(email, set, akTags)) {
                threadSizeData.anyTag ~= threadSize;
                threadParticipationData.anyTag ~= participantCount;
                foreach (tag; akTags) {
                    if (threadHasAk(email, set, [tag])) {
                        threadSizeData.tagData[tag] ~= threadSize;
                        threadParticipationData.tagData[tag] ~= participantCount;
                    }
                }
            } else {
                threadSizeData.notAk ~= threadSize;
                threadParticipationData.notAk ~= participantCount;
            }
        }
    }

    void addToJson(ref JSONValue obj) {
        JSONValue o;
        o["body_size"] = bodySizeData.toJson;
        o["word_count"] = wordCountData.toJson;
        o["thread_size"] = threadSizeData.toJson;
        o["thread_participation"] = threadParticipationData.toJson;
        obj["characteristic"] = o;
    }
}