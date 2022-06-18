package nl.andrewl.emaildatasetreportgen.cmd;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailEntryPreview;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.email_indexer.data.search.EmailIndexSearcher;
import nl.andrewl.emaildatasetreportgen.AkStatus;
import nl.andrewl.emaildatasetreportgen.AnalysisUtils;
import nl.andrewl.emaildatasetreportgen.ReportGen;
import nl.andrewl.emaildatasetreportgen.ReportGenerator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.XYPlot;
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

	@Override
	public void generate(Path outputPath, EmailDataset ds) throws Exception {
		Path myDir = outputPath.resolve("precision");
		Files.createDirectory(myDir);

		XYSeriesCollection collection = new XYSeriesCollection();
		Map<String, XYSeriesCollection> tagCollections = new HashMap<>();

		var queries = ReportGen.getQueries();
		for (var entry : queries.entrySet()) {
			String queryName = entry.getKey();
			String query = entry.getValue();



			// Compute general NDCG for any AK tag.
			XYSeries series = doIterativeNDCG(ds, query, myDir, queryName + "_all_tags_ndcg", ReportGen.POSITIVE_TAGS);
			series.setKey(queryName);
			collection.addSeries(series);

			// Compute one per AK tag.
			for (String tag : ReportGen.POSITIVE_TAGS) {
				XYSeries tagSeries = doIterativeNDCG(ds, query, myDir, queryName + "_" + tag, Collections.singleton(tag));
				tagSeries.setKey(queryName);
				XYSeriesCollection tagCollection = tagCollections.computeIfAbsent(tag, s -> new XYSeriesCollection());
				tagCollection.addSeries(tagSeries);
			}
		}

		// Combined chart for all tags.
		Path allNdcgsFile = myDir.resolve("chart_all.png");
		JFreeChart chart = ChartFactory.createXYLineChart("NDCG", "Iteration Count (N)", "NDCG Measure", collection);
		ChartUtils.saveChartAsPNG(allNdcgsFile.toFile(), chart, 500, 500);

		// Combined chart for each tag.
		for (var entry : tagCollections.entrySet()) {
			String tag = entry.getKey();
			XYSeriesCollection col = entry.getValue();
			JFreeChart c2 = ChartFactory.createXYLineChart("NDCG for tag \"" + tag + "\"", "Iteration Count (N)", "NDCG Measure", col);
			Path file = myDir.resolve("chart_" + tag + ".png");
			ChartUtils.saveChartAsPNG(file.toFile(), c2, 500, 500);
		}
	}

	private XYSeries doIterativeNDCG(EmailDataset ds, String query, Path dir, String name, Collection<String> positiveTags) throws IOException, ParseException {
		System.out.printf("Computing NDCG %s using tags %s%n", name, positiveTags);
		double[] rel = getRelevances(ds, query, positiveTags, ReportGen.NEGATIVE_TAGS);

		CSVFormat format = CSVFormat.Builder.create(CSVFormat.RFC4180)
				.setHeader("N", "DCG", "NDCG")
				.build();
		CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(dir.resolve(name + ".csv")), format);
		XYSeries series = new XYSeries(name);

		for (int n = 1; n <= rel.length; n++) {
			double[] slice = new double[n];
			System.arraycopy(rel, 0, slice, 0, n);
			double dcg = AnalysisUtils.discountedCumulativeGain(slice);
			double ndcg = AnalysisUtils.normalizedDiscountedCumulativeGain(slice);
			printer.printRecord(n, dcg, ndcg);
			series.add(n, ndcg);
		}
		printer.close();

//		createNdcgChart(new XYSeriesCollection(series), "NDCG - " + name, dir.resolve(name + ".png"));
		return series;
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
