package nl.andrewl.emaildatasetreportgen.cmd;

import com.google.gson.*;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.MutationEntry;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.email_indexer.data.search.EmailIndexSearcher;
import nl.andrewl.emaildatasetreportgen.AnalysisUtils;
import nl.andrewl.emaildatasetreportgen.Filters;
import nl.andrewl.emaildatasetreportgen.ReportGen;
import nl.andrewl.emaildatasetreportgen.ReportGenerator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Simple generator that serializes every tagged email as a JSON object and writes an array to a file.
 */
public class JsonDataGenerator implements ReportGenerator {
	public static final int MIN_LEMMA_COUNT = 10;

	@Override
	public void generate(Path outputPath, EmailDataset ds) throws Exception {
		System.out.println("Generating JSON export.");
		exportAllEmails(ds, outputPath);
		exportSearches(ds, outputPath);
		exportMutations(ds, outputPath);
		exportNLP(ds, outputPath);
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

	private void exportMutations(EmailDataset ds, Path outputPath) throws IOException {
		System.out.println("Exporting mutations.");
		JsonArray mutationsArray = new JsonArray();
		for (MutationEntry mutation : new EmailRepository(ds).getAllMutations()) {
			JsonObject obj = new JsonObject();
			obj.addProperty("id", mutation.id());
			obj.addProperty("affected_email_count", mutation.affectedEmailCount());
			obj.addProperty("description", mutation.description());
			mutationsArray.add(obj);
		}
		Path mutationsFile = outputPath.resolve("mutations.json");
		writeJson(mutationsArray, mutationsFile);
	}

	private void exportNLP(EmailDataset ds, Path outputPath) throws IOException, InterruptedException {
		System.out.println("Exporting NLP lemmatization data.");
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
		props.setProperty("coref.algorithm", "neural");

		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		Map<Long, CoreDocument> emailData = new HashMap<>();
		Map<String, Set<Long>> taggedEmails = new HashMap<>();

		// Run the document annotation in parallel, since the pipeline is threadsafe, and it's really slow.
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		AnalysisUtils.doForAllEmails(ds, Filters.taggedEmails(new TagRepository(ds)), (email, tags) -> {
			executor.submit(() -> {
				CoreDocument doc = new CoreDocument(email.body());
				pipeline.annotate(doc);
				emailData.put(email.id(), doc);
				for (var tag : tags) {
					Set<Long> emailIds = taggedEmails.computeIfAbsent(tag.name(), s -> new HashSet<>());
					emailIds.add(email.id());
				}
			});
		});
		executor.shutdown();
		boolean finished;
		do {
			finished = executor.awaitTermination(1, TimeUnit.MINUTES);
		} while (!finished);

		JsonObject lemmaJson = new JsonObject();
		JsonObject allTags = getLemmaData(Set.of("existence", "technology", "process", "property"), emailData, taggedEmails);
		JsonObject existence = getLemmaData(Set.of("existence"), emailData, taggedEmails);
		JsonObject technology = getLemmaData(Set.of("technology"), emailData, taggedEmails);
		JsonObject process = getLemmaData(Set.of("process"), emailData, taggedEmails);
		JsonObject property = getLemmaData(Set.of("property"), emailData, taggedEmails);
		JsonObject notAk = getLemmaData(Set.of("not-ak"), emailData, taggedEmails);
		lemmaJson.add("all_tags", allTags);
		lemmaJson.add("existence", existence);
		lemmaJson.add("technology", technology);
		lemmaJson.add("process", process);
		lemmaJson.add("property", property);
		lemmaJson.add("not_ak", notAk);
		writeJson(lemmaJson, outputPath.resolve("lemmas.json"));
	}

	private static JsonObject getLemmaData(Set<String> tags, Map<Long, CoreDocument> documents, Map<String, Set<Long>> taggedEmailIds) throws IOException {
		JsonObject obj = new JsonObject();
		JsonArray tagsArray = new JsonArray(tags.size());
		tags.stream().sorted().forEachOrdered(tagsArray::add);
		obj.add("tags", tagsArray);
		// Find all applicable emails that have at least one of the specified tags.
		Set<Long> applicableEmailIds = new HashSet<>();
		for (var entry : taggedEmailIds.entrySet()) {
			if (tags.contains(entry.getKey())) applicableEmailIds.addAll(entry.getValue());
		}
		// Find all applicable documents for emails that have the right tags.
		List<CoreDocument> applicableDocuments = new ArrayList<>();
		for (var emailId : applicableEmailIds) {
			CoreDocument doc = documents.get(emailId);
			if (doc != null) applicableDocuments.add(doc);
		}
		// Count up all the lemmas in all applicable documents.
		Map<String, Integer> lemmaCounts = new HashMap<>();
		for (var doc : applicableDocuments) {
			for (var token : doc.tokens()) {
				if (!getStopWords().contains(token.lemma().toLowerCase())) {
					int count = lemmaCounts.computeIfAbsent(token.lemma(), s -> 0);
					lemmaCounts.put(token.lemma(), count + 1);
				}
			}
		}

		// Serialize the data into a JSON object with key-value pairs being the lemmas and their frequencies.
		JsonObject lemmasObj = new JsonObject();
		List<Map.Entry<String, Integer>> values = lemmaCounts.entrySet().stream()
				.filter(entry -> entry.getValue() >= MIN_LEMMA_COUNT) // Filter out all the garbage lemmas that don't occur much.
				.sorted(Collections.reverseOrder(Comparator.comparingInt(Map.Entry::getValue)))
				.toList();
		for (var value : values) {
			lemmasObj.addProperty(value.getKey(), value.getValue());
		}
		obj.add("lemmas", lemmasObj);
		return obj;
	}

	private static Set<String> stopWords = null;
	private static Set<String> getStopWords() throws IOException {
		if (stopWords == null) {
			try (var in = JsonDataGenerator.class.getClassLoader().getResourceAsStream("stopwords-en.txt")) {
				if (in == null) throw new IOException("Couldn't load stopwords file from resources.");
				var reader = new BufferedReader(new InputStreamReader(in));
				String line;
				stopWords = new HashSet<>();
				while ((line = reader.readLine()) != null) {
					stopWords.add(line.strip());
				}
			}
		}
		return stopWords;
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
