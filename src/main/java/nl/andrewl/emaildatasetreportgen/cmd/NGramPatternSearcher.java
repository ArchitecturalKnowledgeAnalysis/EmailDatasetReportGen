package nl.andrewl.emaildatasetreportgen.cmd;

import nl.andrewl.email_indexer.data.EmailEntry;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.Tag;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.emaildatasetreportgen.DatasetEmailConsumer;
import nl.andrewl.emaildatasetreportgen.ReportGen;

import java.util.*;

public class NGramPatternSearcher implements DatasetEmailConsumer {
	public record Result(List<String> pattern, int count) {}

	private final List<List<String>> possiblePatterns;
	private final Map<List<String>, Integer> results;
	private final EmailRepository emailRepo;
	private final TagRepository tagRepo;
	private final boolean skipNotAk;

	public NGramPatternSearcher(List<List<String>> possiblePatterns, EmailRepository emailRepo, TagRepository tagRepo, boolean skipNotAk) {
		this.possiblePatterns = possiblePatterns;
		this.emailRepo = emailRepo;
		this.tagRepo = tagRepo;
		this.skipNotAk = skipNotAk;
		this.results = new HashMap<>();
		for (List<String> pattern : possiblePatterns) {
			results.put(pattern, 0);
		}
	}

	public Map<List<String>, Integer> getResults() {
		return results;
	}

	public List<Result> getResultsOrdered() {
		List<Result> resultsList = new ArrayList<>(results.size());
		for (var entry : results.entrySet()) {
			resultsList.add(new Result(entry.getKey(), entry.getValue()));
		}
		resultsList.sort(Comparator.comparingInt(Result::count).reversed());
		return resultsList;
	}

	@Override
	public void consumeEmail(EmailEntry email, Collection<Tag> tags) throws Exception {
		patternSearchRecursive(email.id());
	}

	private void patternSearchRecursive(long emailId) {
		List<String> tags = tagRepo.getTags(emailId).stream().map(Tag::name).toList();
		for (String tag : tags) {
			for (List<String> pattern : possiblePatterns) {
				if (pattern.size() > 0 && pattern.get(0).equals(tag) && hasPattern(emailId, pattern)) {
					results.put(pattern, results.get(pattern) + 1);
				}
			}
		}
		for (long replyId : emailRepo.findAllReplyIds(emailId)) {
			patternSearchRecursive(replyId);
		}
	}

	/**
	 * Recursively checks if an email thread beginning with the given id
	 * matches the given tag pattern.
	 * @param emailId The id of the first email in the thread to search.
	 * @param pattern The pattern to look for.
	 * @return True if the email and its replies match the given tag pattern.
	 */
	private boolean hasPattern(long emailId, List<String> pattern) {
		if (pattern.size() <= 0) {
			return true;
		}
		List<String> tags = tagRepo.getTags(emailId).stream().map(Tag::name).toList();
		boolean thisEmailMatches = tags.contains(pattern.get(0));
		// If we're allowed to skip non-AK emails, and this email is non-AK, then skip it and see if any replies match.
		if (skipNotAk && !thisEmailMatches && tags.stream().anyMatch(ReportGen.NEGATIVE_TAGS::contains)) {
			return emailRepo.findAllReplyIds(emailId).stream()
					.anyMatch(replyId -> hasPattern(replyId, pattern));
		}
		if (pattern.size() == 1) {
			return thisEmailMatches;
		} else {
			boolean anyChildMatches = emailRepo.findAllReplyIds(emailId).stream()
					.anyMatch(replyId -> hasPattern(replyId, pattern.subList(1, pattern.size())));
			return thisEmailMatches && anyChildMatches;
		}
	}
}
