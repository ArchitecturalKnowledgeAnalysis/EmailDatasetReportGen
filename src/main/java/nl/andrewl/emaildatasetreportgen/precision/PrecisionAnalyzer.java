package nl.andrewl.emaildatasetreportgen.precision;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.email_indexer.data.search.EmailIndexSearcher;
import nl.andrewl.emaildatasetreportgen.AnalysisUtils;
import nl.andrewl.emaildatasetreportgen.relevance.RelevanceAnalyzer;

import java.util.Collection;

public class PrecisionAnalyzer {
	private final EmailDataset dataset;
	private final Collection<String> positiveTags;
	private final Collection<String> negativeTags;
	private final int searchResultSize;

	public PrecisionAnalyzer(EmailDataset ds, Collection<String> positiveTags, Collection<String> negativeTags, int searchResultSize) {
		this.dataset = ds;
		this.positiveTags = positiveTags;
		this.negativeTags = negativeTags;
		this.searchResultSize = searchResultSize;
	}

	public PrecisionResult analyzeSearch(String query) throws Exception {
		var emailRepo = new EmailRepository(dataset);
		var tagRepo = new TagRepository(dataset);
		RelevanceAnalyzer relevanceAnalyzer = new RelevanceAnalyzer(emailRepo, tagRepo, positiveTags, negativeTags);
		double[] relevances = new EmailIndexSearcher().search(dataset, query, searchResultSize).stream()
				.mapToDouble(relevanceAnalyzer::analyzeThread).toArray();
		double[] ndcgValues = new double[searchResultSize];
		double[] precisionValues = new double[searchResultSize];
		for (int i = 0; i < relevances.length; i++) {
			final int n = i + 1;
			double[] slice = new double[n];
			System.arraycopy(relevances, 0, slice, 0, n);

			ndcgValues[i] = AnalysisUtils.normalizedDiscountedCumulativeGain(slice);

			int totalPrecision = 0;
			for (double rel : slice) {
				if (rel > 0) totalPrecision++;
			}
			precisionValues[i] = totalPrecision / (double) n;
		}
		return new PrecisionResult(ndcgValues, precisionValues);
	}
}
