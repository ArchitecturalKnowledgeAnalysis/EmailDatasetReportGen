package nl.andrewl.emaildatasetreportgen.cmd;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailEntryPreview;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.email_indexer.data.search.EmailIndexSearcher;
import nl.andrewl.emaildatasetreportgen.AkStatus;
import nl.andrewl.emaildatasetreportgen.AnalysisUtils;
import nl.andrewl.emaildatasetreportgen.ReportGen;
import picocli.CommandLine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CommandLine.Command(
		name = "top-threads",
		description = "Get information about the top email threads for a given search query."
)
public class TopThreads implements Runnable {
	@CommandLine.ParentCommand
	private ReportGen parent;

	@CommandLine.Option(names = {"-q", "--query"}, description = "The Lucene query to use to search.", required = true)
	private String query;

	@CommandLine.Option(names = {"-n", "--thread-count"}, description = "The number of threads to analyze.", defaultValue = "100")
	private int threadCount;

	@Override
	public void run() {
		EmailDataset ds = parent.getDataset();
		try {
			List<Long> ids = new EmailIndexSearcher().search(ds, query, threadCount);
			EmailRepository repo = new EmailRepository(ds);
			TagRepository tagRepo = new TagRepository(ds);
			for (int i = 0; i < ids.size(); i++) {
				System.out.println("Thread " + (i + 1) + ": weight: " + getAkWeight(ids.get(i), repo, tagRepo));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ds.close().join();
		}
	}

	private float getAkWeight(long id, EmailRepository repo, TagRepository tagRepo) {
		Map<AkStatus, Integer> counts = new HashMap<>();
		analyzeEmailRecursive(id, repo, tagRepo, counts);
		int akCount = counts.computeIfAbsent(AkStatus.ARCHITECTURAL, s -> 0);
		int notAkCount = counts.computeIfAbsent(AkStatus.NOT_ARCHITECTURAL, s -> 0);
		int categorizedCount = akCount + notAkCount;
		if (categorizedCount == 0) return 0;
		return (float) akCount / (float) categorizedCount;
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
