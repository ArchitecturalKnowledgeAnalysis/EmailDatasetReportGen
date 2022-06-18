package nl.andrewl.emaildatasetreportgen.cmd;

import nl.andrewl.email_indexer.data.*;
import nl.andrewl.email_indexer.data.search.EmailIndexSearcher;
import nl.andrewl.emaildatasetreportgen.AkStatus;
import nl.andrewl.emaildatasetreportgen.AnalysisUtils;
import nl.andrewl.emaildatasetreportgen.ReportGen;
import nl.andrewl.emaildatasetreportgen.ReportGenerator;
import org.apache.lucene.queryparser.classic.ParseException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Generates report data for search precision stuff.
 */
public class PrecisionReportGenerator implements ReportGenerator {
	private static final int MAX_RESULTS = 75;

	private record PrecisionResults(XYSeries ndcg, XYSeries precision) {}


	@Override
	public void generate(Path outputPath, EmailDataset ds) throws Exception {
		Path myDir = outputPath.resolve("precision");
		Files.createDirectory(myDir);
		List<String> tags = ReportGen.POSITIVE_TAGS.stream().sorted().toList();

		XYSeriesCollection ndcgCollection = new XYSeriesCollection();
		Map<String, XYSeriesCollection> tagNdcgCollections = new HashMap<>();

		XYSeriesCollection precisionCollection = new XYSeriesCollection();
		Map<String, XYSeriesCollection> tagPrecisionCollections = new HashMap<>();

		var queries = ReportGen.getQueries();
		for (var entry : queries.entrySet()) {
			String queryName = entry.getKey();
			String query = entry.getValue();

			// Compute general NDCG for any AK tag.
			PrecisionResults allTagsResults = doIterativeNDCG(ds, query, queryName + "_all_tags_ndcg", ReportGen.POSITIVE_TAGS);
			allTagsResults.ndcg.setKey(queryName);
			allTagsResults.precision.setKey(queryName);
			ndcgCollection.addSeries(allTagsResults.ndcg);
			precisionCollection.addSeries(allTagsResults.precision);

			// Compute one per AK tag.
			for (String tag : ReportGen.POSITIVE_TAGS) {
				PrecisionResults specificTagResults = doIterativeNDCG(ds, query, queryName + "_" + tag, Collections.singleton(tag));
				specificTagResults.ndcg.setKey(queryName);
				specificTagResults.precision.setKey(queryName);
				tagNdcgCollections.computeIfAbsent(tag, s -> new XYSeriesCollection()).addSeries(specificTagResults.ndcg);
				tagPrecisionCollections.computeIfAbsent(tag, s -> new XYSeriesCollection()).addSeries(specificTagResults.precision);
			}
		}

		// Combined chart for all tags.
		JFreeChart ndcgChartAll = ChartFactory.createXYLineChart("NDCG for All Tags", "Result Count (N)", "NDCG Measure", ndcgCollection);
		ChartUtils.saveChartAsPNG(myDir.resolve("ndcg_all.png").toFile(), ndcgChartAll, 500, 500);

		JFreeChart precisionChartAll = ChartFactory.createXYLineChart("Precision for All Tags", "Result Count (N)", "Precision", precisionCollection);
		ChartUtils.saveChartAsPNG(myDir.resolve("precision_all.png").toFile(), precisionChartAll, 500, 500);

		// Combined chart for each tag.
		for (var tag : tags) {
			XYSeriesCollection ndcgTagCollection = tagNdcgCollections.get(tag);
			JFreeChart ndcgTagChart = ChartFactory.createXYLineChart("NDCG for Tag \"" + tag + "\"", "Result Count (N)", "NDCG Measure", ndcgTagCollection);
			ChartUtils.saveChartAsPNG(myDir.resolve("ndcg_" + tag + ".png").toFile(), ndcgTagChart, 500, 500);

			XYSeriesCollection precisionTagCollection = tagPrecisionCollections.get(tag);
			JFreeChart precisionTagChart = ChartFactory.createXYLineChart("Precision for Tag \"" + tag + "\"", "Result Count (N)", "Precision", precisionTagCollection);
			ChartUtils.saveChartAsPNG(myDir.resolve("precision_" + tag + ".png").toFile(), precisionTagChart, 500, 500);
		}
	}

	private PrecisionResults doIterativeNDCG(EmailDataset ds, String query, String name, Collection<String> positiveTags) throws IOException, ParseException {
		System.out.printf("Computing NDCG %s using tags %s%n", name, positiveTags);
		double[] rel = getRelevances(ds, query, positiveTags, ReportGen.NEGATIVE_TAGS);
		XYSeries ndcgSeries = new XYSeries(name);
		XYSeries precisionSeries = new XYSeries(name);
		for (int n = 1; n <= rel.length; n++) {
			double[] slice = new double[n];
			System.arraycopy(rel, 0, slice, 0, n);
//			double dcg = AnalysisUtils.discountedCumulativeGain(slice);
			double ndcg = AnalysisUtils.normalizedDiscountedCumulativeGain(slice);
			ndcgSeries.add(n, ndcg);
			double avgPrecision = AnalysisUtils.avg(slice);
			precisionSeries.add(n, avgPrecision);
		}
		return new PrecisionResults(ndcgSeries, precisionSeries);
	}

	private double[] getRelevances(EmailDataset ds, String query, Collection<String> positiveTags, Collection<String> negativeTags) throws IOException, ParseException {
		var emailRepo = new EmailRepository(ds);
		var tagRepo = new TagRepository(ds);
		return new EmailIndexSearcher().search(ds, query, MAX_RESULTS).stream()
				.mapToDouble(id -> getAkWeight(id, emailRepo, tagRepo, positiveTags, negativeTags))
				.toArray();
	}

	private double getAkWeight(long id, EmailRepository repo, TagRepository tagRepo, Collection<String> positiveTags, Collection<String> negativeTags) {
		Map<AkStatus, Integer> counts = new HashMap<>();
		analyzeEmailRecursive(id, repo, tagRepo, positiveTags, negativeTags, counts);
		int akCount = counts.computeIfAbsent(AkStatus.ARCHITECTURAL, s -> 0);
		int notAkCount = counts.computeIfAbsent(AkStatus.NOT_ARCHITECTURAL, s -> 0);
		int categorizedCount = akCount + notAkCount;
		if (categorizedCount == 0) return 0;
		return (double) akCount / (double) categorizedCount;
	}

	private void analyzeEmailRecursive(long id, EmailRepository repo, TagRepository tagRepo, Collection<String> positiveTags, Collection<String> negativeTags, Map<AkStatus, Integer> counts) {
		AkStatus status = AnalysisUtils.getStatus(id, tagRepo, positiveTags, negativeTags);
		int count = counts.computeIfAbsent(status, s -> 0);
		counts.put(status, count + 1);
		for (EmailEntryPreview reply : repo.findAllReplies(id)) {
			analyzeEmailRecursive(reply.id(), repo, tagRepo, positiveTags, negativeTags, counts);
		}
	}

	private void createNdcgChart(XYDataset ds, String title, Path file) throws IOException {
		JFreeChart chart = ChartFactory.createXYLineChart(title, "N", "Value", ds);
		chart.removeLegend();

		try (var out = Files.newOutputStream(file)) {
			ChartUtils.writeChartAsPNG(out, chart, 500, 500);
		}
	}
}
