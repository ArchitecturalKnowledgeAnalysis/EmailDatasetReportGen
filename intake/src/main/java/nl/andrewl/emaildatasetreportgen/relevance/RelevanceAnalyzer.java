package nl.andrewl.emaildatasetreportgen.relevance;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.emaildatasetreportgen.AnalysisUtils;
import nl.andrewl.emaildatasetreportgen.Filters;

import java.util.*;

public class RelevanceAnalyzer {
	private final EmailRepository emailRepo;
	private final TagRepository tagRepo;
	private final Collection<String> positiveTags;
	private final double maxAkCount;
	private final Map<Long, Double> threadRelevanceCache;

	public RelevanceAnalyzer(EmailRepository emailRepo, TagRepository tagRepo, Collection<String> positiveTags, double maxAkCount) {
		this.emailRepo = emailRepo;
		this.tagRepo = tagRepo;
		this.positiveTags = positiveTags;
		this.maxAkCount = maxAkCount;
		this.threadRelevanceCache = new HashMap<>();
	}

	public RelevanceAnalyzer(EmailDataset ds, Collection<String> positiveTags) {
		this(new EmailRepository(ds), new TagRepository(ds), positiveTags, getMaxAkCount(ds, positiveTags));
	}

	public double getMaxAkCount() {
		return maxAkCount;
	}

	/**
	 * Computes the relevance of an email thread, which is the normalized
	 * amount of emails containing architectural knowledge, relative to the
	 * maximum amount of emails found in a thread in the dataset.
	 * @param emailId The id of the root email in the thread.
	 * @return The thread's relevance, from 0 to 1, inclusive.
	 */
	public double analyzeThread(long emailId) {
		Double relevance = threadRelevanceCache.get(emailId);
		if (relevance != null) {
			return relevance;
		}
		int akCount = countTagsRecursive(emailId, tagRepo, emailRepo, positiveTags);
		double rel = Math.min(1.0, akCount / maxAkCount); // Normalize AK count against the established maximum.
		threadRelevanceCache.put(emailId, rel);
		return rel;
	}

	public double analyzeEmail(long emailId) {
		boolean hasAk = tagRepo.getTags(emailId).stream()
				.anyMatch(tag -> positiveTags.contains(tag.name()));
		if (hasAk) {
			return 1.0;
		} else {
			return 0.0;
		}
	}

	public static double getMaxAkCount(EmailDataset ds, Collection<String> positiveTags) {
		var tagRepo = new TagRepository(ds);
		var emailRepo = new EmailRepository(ds);
		List<Integer> akCounts = new ArrayList<>();
		AnalysisUtils.doForAllEmails(ds, Filters.taggedThreads(tagRepo), (email, tags) -> {
			akCounts.add(countTagsRecursive(email.id(), tagRepo, emailRepo, positiveTags));
		});
		System.out.println(akCounts.stream().sorted(Comparator.reverseOrder()).limit(10).toList());
		return akCounts.stream()
				.sorted(Comparator.reverseOrder())
				.mapToDouble(v -> v)
				.limit(10)
				.average().orElseThrow();
	}

	private static int countTagsRecursive(long emailId, TagRepository tagRepo, EmailRepository emailRepo, Collection<String> positiveTags) {
		int count = (int) tagRepo.getTags(emailId).stream()
				.filter(tag -> positiveTags.contains(tag.name()))
				.count();
		for (long replyId : emailRepo.findAllReplyIds(emailId)) {
			count += countTagsRecursive(replyId, tagRepo, emailRepo, positiveTags);
		}
		return count;
	}
}
