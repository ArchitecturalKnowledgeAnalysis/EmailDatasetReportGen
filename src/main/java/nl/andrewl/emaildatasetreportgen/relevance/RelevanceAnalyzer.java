package nl.andrewl.emaildatasetreportgen.relevance;

import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.emaildatasetreportgen.AkStatus;
import nl.andrewl.emaildatasetreportgen.AnalysisUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RelevanceAnalyzer {
	private final EmailRepository emailRepo;
	private final TagRepository tagRepo;
	private final Collection<String> positiveTags;
	private final Collection<String> negativeTags;

	public RelevanceAnalyzer(EmailRepository emailRepo, TagRepository tagRepo, Collection<String> positiveTags, Collection<String> negativeTags) {
		this.emailRepo = emailRepo;
		this.tagRepo = tagRepo;
		this.positiveTags = positiveTags;
		this.negativeTags = negativeTags;
	}

	/**
	 * Computes the relevance of an email thread, which is defined as the ratio
	 * of architectural emails to non-architectural emails.
	 * @param emailId The id of the root email in the thread.
	 * @return The thread's relevance, from 0 to 1, inclusive.
	 */
	public double analyzeThread(long emailId) {
		Map<AkStatus, Integer> counts = new HashMap<>();
		analyzeEmailRecursive(emailId, counts);
		int akCount = counts.computeIfAbsent(AkStatus.ARCHITECTURAL, s -> 0);
		int notAkCount = counts.computeIfAbsent(AkStatus.NOT_ARCHITECTURAL, s -> 0);
		int categorizedCount = akCount + notAkCount;
		if (categorizedCount == 0) return 0;
		return (double) akCount / (double) categorizedCount;
	}

	private void analyzeEmailRecursive(long id, Map<AkStatus, Integer> counts) {
		AkStatus status = AnalysisUtils.getStatus(id, tagRepo, positiveTags, negativeTags);
		int count = counts.computeIfAbsent(status, s -> 0);
		counts.put(status, count + 1);
		for (long replyId : emailRepo.findAllReplyIds(id)) {
			analyzeEmailRecursive(replyId, counts);
		}
	}
}
