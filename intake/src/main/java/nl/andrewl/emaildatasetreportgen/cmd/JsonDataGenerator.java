package nl.andrewl.emaildatasetreportgen.cmd;

import com.google.gson.*;
import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.email_indexer.data.search.EmailIndexSearcher;
import nl.andrewl.emaildatasetreportgen.AnalysisUtils;
import nl.andrewl.emaildatasetreportgen.Filters;
import nl.andrewl.emaildatasetreportgen.ReportGen;
import nl.andrewl.emaildatasetreportgen.ReportGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

/**
 * Simple generator that serializes every tagged email as a JSON object and writes an array to a file.
 */
public class JsonDataGenerator implements ReportGenerator {
	@Override
	public void generate(Path outputPath, EmailDataset ds) throws Exception {
		System.out.println("Generating JSON export.");
		exportAllEmails(ds, outputPath);
		exportSearches(ds, outputPath);
		System.out.println("JSON export complete.");
	}

	private void exportSearches(EmailDataset ds, Path outputPath) throws Exception {
		System.out.println("Exporting Lucene search results.");
		EmailIndexSearcher searcher = new EmailIndexSearcher();
		JsonArray searchData = new JsonArray();
		for (var entry : ReportGen.getQueries().entrySet()) {
			JsonObject queryData = new JsonObject();
			String queryName = entry.getKey();
			String query = entry.getValue();
			queryData.addProperty("name", queryName);
			queryData.addProperty("query", query);
			JsonArray threadsArray = new JsonArray(75);
			for (var id : searcher.search(ds, query, 75)) threadsArray.add(id);
			JsonArray emailsArray = new JsonArray(1800);
			for (var id : searcher.searchEmails(ds, query, 1800)) emailsArray.add(id);
			queryData.add("threads", threadsArray);
			queryData.add("emails", emailsArray);
			searchData.add(queryData);
		}
		writeJson(searchData, outputPath.resolve("searches.json"));
	}

	private void exportAllEmails(EmailDataset ds, Path outputPath) throws IOException {
		System.out.println("Exporting all emails.");
		JsonArray emailsArray = new JsonArray();
		AnalysisUtils.doForAllEmails(ds, Filters.taggedEmails(new TagRepository(ds)), (email, tags) -> {
			JsonObject obj = new JsonObject();
			obj.addProperty("id", email.id());
			obj.addProperty("parent_id", email.parentId());
			obj.addProperty("message_id", email.messageId());
			obj.addProperty("subject", email.subject());
			obj.addProperty("in_reply_to", email.inReplyTo());
			obj.addProperty("sent_from", email.sentFrom());
			obj.addProperty("date", email.date().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
			obj.addProperty("body", email.body());
			JsonArray tagsArray = new JsonArray(tags.size());
			for (var tag : tags) tagsArray.add(tag.name());
			obj.add("tags", tagsArray);
			emailsArray.add(obj);
		});
		System.out.println("Writing emails to file.");
		Path emailsFile = outputPath.resolve("emails.json");
		writeJson(emailsArray, emailsFile);
	}

	private static void writeJson(JsonElement j, Path file) throws IOException {
		try (var out = Files.newBufferedWriter(file)) {
			Gson gson = new GsonBuilder()
					.serializeNulls()
					.setPrettyPrinting()
					.create();
			gson.toJson(j, out);
		}
	}
}
