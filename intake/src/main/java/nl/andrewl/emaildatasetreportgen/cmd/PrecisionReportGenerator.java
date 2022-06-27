package nl.andrewl.emaildatasetreportgen.cmd;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.email_indexer.data.search.EmailIndexSearcher;
import nl.andrewl.emaildatasetreportgen.AnalysisUtils;
import nl.andrewl.emaildatasetreportgen.Filters;
import nl.andrewl.emaildatasetreportgen.ReportGen;
import nl.andrewl.emaildatasetreportgen.ReportGenerator;
import nl.andrewl.emaildatasetreportgen.precision.PrecisionAnalyzer;
import nl.andrewl.emaildatasetreportgen.precision.PrecisionResult;
import nl.andrewl.emaildatasetreportgen.relevance.RelevanceAnalyzer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Generates report data for search precision stuff.
 */
public class PrecisionReportGenerator implements ReportGenerator {
	private static final int MAX_THREADS = 75;
	private static final int MAX_EMAILS = 500;

	private final RelevanceAnalyzer relevanceAnalyzer;

	public PrecisionReportGenerator(RelevanceAnalyzer relevanceAnalyzer) {
		this.relevanceAnalyzer = relevanceAnalyzer;
	}

	@Override
	public void generate(Path outputPath, EmailDataset ds) throws Exception {
		Path myDir = outputPath.resolve("precision");
		Files.createDirectory(myDir);
		List<String> tags = ReportGen.POSITIVE_TAGS.stream().sorted().toList();

		// Pre-compute the ideal search results for the dataset, being the threads with the most relevance.
		System.out.println("Pre-computing ideal relevance values for precision analysis.");
		List<Double> threadRelevances = new ArrayList<>();
		AnalysisUtils.doForAllEmails(ds, Filters.taggedThreads(new TagRepository(ds)), (email, tags1) -> {
			threadRelevances.add(relevanceAnalyzer.analyzeThread(email.id()));
		});
		List<Double> emailRelevances = new ArrayList<>();
		AnalysisUtils.doForAllEmails(ds, Filters.taggedEmails(new TagRepository(ds)), (email, tags1) -> {
			emailRelevances.add(relevanceAnalyzer.analyzeEmail(email.id()));
		});
		double[] threadIdealRelevances = threadRelevances.stream()
				.sorted(Comparator.reverseOrder())
				.mapToDouble(v -> v)
				.limit(MAX_THREADS)
				.toArray();
		double[] emailIdealRelevances = emailRelevances.stream()
				.sorted(Comparator.reverseOrder())
				.mapToDouble(v -> v)
				.limit(MAX_EMAILS)
				.toArray();

		PrecisionAnalyzer allTagsPrecisionAnalyzer = new PrecisionAnalyzer(
				query -> new EmailIndexSearcher()
						.search(ds, query, MAX_THREADS)
						.stream()
						.mapToDouble(relevanceAnalyzer::analyzeThread)
						.toArray(),
				threadIdealRelevances
		);
		PrecisionAnalyzer allTagsEmailPrecisionAnalyzer = new PrecisionAnalyzer(
				query -> new EmailIndexSearcher()
						.searchEmails(ds, query, MAX_EMAILS)
						.stream()
						.mapToDouble(relevanceAnalyzer::analyzeEmail)
						.toArray(),
				emailIdealRelevances
		);

		XYSeriesCollection ndcgCollection = new XYSeriesCollection();
		XYSeriesCollection emailsNdcgCollection = new XYSeriesCollection();
		Map<String, XYSeriesCollection> tagNdcgCollections = new HashMap<>();

		XYSeriesCollection precisionCollection = new XYSeriesCollection();
		XYSeriesCollection emailsPrecisionCollection = new XYSeriesCollection();
		Map<String, XYSeriesCollection> tagPrecisionCollections = new HashMap<>();

		var queries = ReportGen.getQueries();
		for (var entry : queries.entrySet()) {
			String queryName = entry.getKey();
			String query = entry.getValue();
			Path queryDir = myDir.resolve(queryName);
			Files.createDirectory(queryDir);

			// Compute general NDCG for any AK tag.
			PrecisionResult allTagsResult = allTagsPrecisionAnalyzer.analyzeSearch(query);
			PrecisionResult allTagsEmailResult = allTagsEmailPrecisionAnalyzer.analyzeSearch(query);
			savePrecisionCSV(queryDir.resolve("all-tags.csv"), allTagsResult);
			savePrecisionCSV(queryDir.resolve("all-tags_emails.csv"), allTagsEmailResult);

			// Add data for charting.
			XYSeries allTagsNdcgSeries = allTagsResult.ndcgSeries();
			XYSeries allTagsPrecisionSeries = allTagsResult.precisionSeries();
			allTagsNdcgSeries.setKey(queryName);
			allTagsPrecisionSeries.setKey(queryName);
			ndcgCollection.addSeries(allTagsNdcgSeries);
			precisionCollection.addSeries(allTagsPrecisionSeries);

			XYSeries allTagsEmailNdcgSeries = allTagsEmailResult.ndcgSeries();
			allTagsEmailNdcgSeries.setKey(queryName);
			emailsNdcgCollection.addSeries(allTagsEmailNdcgSeries);
			XYSeries allTagsEmailPrecisionSeries = allTagsEmailResult.precisionSeries();
			allTagsEmailPrecisionSeries.setKey(queryName);
			emailsPrecisionCollection.addSeries(allTagsEmailPrecisionSeries);

			// Compute one per AK tag.
			for (String tag : ReportGen.POSITIVE_TAGS) {
				RelevanceAnalyzer tagRelevanceAnalyzer = new RelevanceAnalyzer(
						new EmailRepository(ds),
						new TagRepository(ds),
						Collections.singleton(tag),
						relevanceAnalyzer.getMaxAkCount()
				);
				PrecisionAnalyzer tagPrecisionAnalyzer = new PrecisionAnalyzer(
						q -> new EmailIndexSearcher()
								.search(ds, q, MAX_THREADS)
								.stream()
								.mapToDouble(tagRelevanceAnalyzer::analyzeThread)
								.toArray(),
						threadIdealRelevances
				);
				PrecisionResult tagResult = tagPrecisionAnalyzer.analyzeSearch(query);
				savePrecisionCSV(queryDir.resolve(tag + ".csv"), tagResult);

				// Data for charting.
				XYSeries tagNdcgSeries = tagResult.ndcgSeries();
				XYSeries tagPrecisionSeries = tagResult.precisionSeries();
				tagNdcgSeries.setKey(queryName);
				tagPrecisionSeries.setKey(queryName);
				tagNdcgCollections.computeIfAbsent(tag, s -> new XYSeriesCollection()).addSeries(tagNdcgSeries);
				tagPrecisionCollections.computeIfAbsent(tag, s -> new XYSeriesCollection()).addSeries(tagPrecisionSeries);
			}
		}

		// Combined chart for all tags.
		JFreeChart ndcgChartAll = ChartFactory.createXYLineChart("NDCG for All Tags", "Result Count (N)", "NDCG Measure", ndcgCollection);
		ChartUtils.saveChartAsPNG(myDir.resolve("ndcg_all.png").toFile(), ndcgChartAll, 1000, 500);

		JFreeChart emailNdcgChartAll = ChartFactory.createXYLineChart("NDCG for All Tags, Individual Emails", "Result Count (N)", "NDCG Measure", emailsNdcgCollection);
		ChartUtils.saveChartAsPNG(myDir.resolve("ndcg_all_emails.png").toFile(), emailNdcgChartAll, 1000, 500);

		JFreeChart precisionChartAll = ChartFactory.createXYLineChart("Precision for All Tags", "Result Count (N)", "Precision", precisionCollection);
		ChartUtils.saveChartAsPNG(myDir.resolve("precision_all.png").toFile(), precisionChartAll, 1000, 500);

		JFreeChart emailPrecisionChartAll = ChartFactory.createXYLineChart("Precision for All Tags, Individual Emails", "Result Count (N)", "Precision", emailsPrecisionCollection);
		ChartUtils.saveChartAsPNG(myDir.resolve("precision_all_emails.png").toFile(), emailPrecisionChartAll, 1000, 500);

		// Combined chart for each tag.
		for (var tag : tags) {
			XYSeriesCollection ndcgTagCollection = tagNdcgCollections.get(tag);
			JFreeChart ndcgTagChart = ChartFactory.createXYLineChart("NDCG for Tag \"" + tag + "\"", "Result Count (N)", "NDCG Measure", ndcgTagCollection);
			ChartUtils.saveChartAsPNG(myDir.resolve("ndcg_" + tag + ".png").toFile(), ndcgTagChart, 1000, 500);

			XYSeriesCollection precisionTagCollection = tagPrecisionCollections.get(tag);
			JFreeChart precisionTagChart = ChartFactory.createXYLineChart("Precision for Tag \"" + tag + "\"", "Result Count (N)", "Precision", precisionTagCollection);
			ChartUtils.saveChartAsPNG(myDir.resolve("precision_" + tag + ".png").toFile(), precisionTagChart, 1000, 500);
		}
	}

	private void savePrecisionCSV(Path file, PrecisionResult result) {
		AnalysisUtils.writeCSV(file, new String[]{"N", "NDCG", "PRECISION"}, printer -> {
			for (int i = 0; i < result.ndcg().length; i++) {
				int n = i + 1;
				printer.printRecord(n, result.ndcg()[i], result.precision()[i]);
			}
		});
	}
}
