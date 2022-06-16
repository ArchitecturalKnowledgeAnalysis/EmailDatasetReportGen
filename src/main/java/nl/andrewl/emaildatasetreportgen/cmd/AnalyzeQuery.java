package nl.andrewl.emaildatasetreportgen.cmd;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailEntryPreview;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.email_indexer.data.search.EmailIndexSearcher;
import nl.andrewl.emaildatasetreportgen.AkStatus;
import nl.andrewl.emaildatasetreportgen.AnalysisUtils;
import nl.andrewl.emaildatasetreportgen.ReportGen;
import org.apache.lucene.queryparser.classic.ParseException;
import picocli.CommandLine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CommandLine.Command(
		name = "analyze-query",
		description = "Compute metrics for a Lucene search query's performance."
)
public class AnalyzeQuery implements Runnable {
	@CommandLine.ParentCommand
	private ReportGen parent;

	@CommandLine.Option(names = {"-q", "--query"}, required = true)
	private String query;

	@CommandLine.Option(names = {"-n", "--result-count"}, description = "The number of results to check.")
	private int resultCount;

	@Override
	public void run() {
		EmailDataset ds = parent.getDataset();
		try {
			List<Long> ids = new EmailIndexSearcher().search(ds, query, resultCount);
			EmailRepository repo = new EmailRepository(ds);
			TagRepository tagRepo = new TagRepository(ds);
			List<Double> values = new ArrayList<>(resultCount);
			for (long id : ids) {
				values.add(getAkWeight(id, repo, tagRepo));
			}
			System.out.println("DCG: " + AnalysisUtils.discountedCumulativeGain(values));
			System.out.println("nDCG: " + AnalysisUtils.normalizedDiscountedCumulativeGain(values));
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		} finally {
			ds.close().join();
		}
	}

	private double getAkWeight(long id, EmailRepository repo, TagRepository tagRepo) {
		Map<AkStatus, Integer> counts = new HashMap<>();
		analyzeEmailRecursive(id, repo, tagRepo, counts);
		int akCount = counts.computeIfAbsent(AkStatus.ARCHITECTURAL, s -> 0);
		int notAkCount = counts.computeIfAbsent(AkStatus.NOT_ARCHITECTURAL, s -> 0);
		int categorizedCount = akCount + notAkCount;
		if (categorizedCount == 0) return 0;
		return (double) akCount / (double) categorizedCount;
	}

	private void analyzeEmailRecursive(long id, EmailRepository repo, TagRepository tagRepo, Map<AkStatus, Integer> counts) {
		AkStatus status = AnalysisUtils.getStatus(id, tagRepo);
		int count = counts.computeIfAbsent(status, s -> 0);
		counts.put(status, count + 1);
		for (EmailEntryPreview reply : repo.findAllReplies(id)) {
			analyzeEmailRecursive(reply.id(), repo, tagRepo, counts);
		}
	}
}
