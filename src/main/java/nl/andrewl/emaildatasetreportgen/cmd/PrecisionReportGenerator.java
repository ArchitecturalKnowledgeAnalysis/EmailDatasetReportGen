package nl.andrewl.emaildatasetreportgen.cmd;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.emaildatasetreportgen.AnalysisUtils;
import nl.andrewl.emaildatasetreportgen.ReportGen;
import nl.andrewl.emaildatasetreportgen.ReportGenerator;
import nl.andrewl.emaildatasetreportgen.precision.PrecisionAnalyzer;
import nl.andrewl.emaildatasetreportgen.precision.PrecisionResult;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates report data for search precision stuff.
 */
public class PrecisionReportGenerator implements ReportGenerator {
	private static final int MAX_RESULTS = 75;

	@Override
	public void generate(Path outputPath, EmailDataset ds) throws Exception {
		Path myDir = outputPath.resolve("precision");
		Files.createDirectory(myDir);
		List<String> tags = ReportGen.POSITIVE_TAGS.stream().sorted().toList();
		PrecisionAnalyzer allTagsPrecisionAnalyzer = new PrecisionAnalyzer(ds, ReportGen.POSITIVE_TAGS, ReportGen.NEGATIVE_TAGS, MAX_RESULTS);

		XYSeriesCollection ndcgCollection = new XYSeriesCollection();
		Map<String, XYSeriesCollection> tagNdcgCollections = new HashMap<>();

		XYSeriesCollection precisionCollection = new XYSeriesCollection();
		Map<String, XYSeriesCollection> tagPrecisionCollections = new HashMap<>();

		var queries = ReportGen.getQueries();
		for (var entry : queries.entrySet()) {
			String queryName = entry.getKey();
			String query = entry.getValue();
			Path queryDir = myDir.resolve(queryName);
			Files.createDirectory(queryDir);

			// Compute general NDCG for any AK tag.
			PrecisionResult allTagsResult = allTagsPrecisionAnalyzer.analyzeSearch(query);
			savePrecisionCSV(queryDir.resolve("all-tags.csv"), allTagsResult);

			// Add data for charting.
			XYSeries allTagsNdcgSeries = allTagsResult.ndcgSeries();
			XYSeries allTagsPrecisionSeries = allTagsResult.precisionSeries();
			allTagsNdcgSeries.setKey(queryName);
			allTagsPrecisionSeries.setKey(queryName);
			ndcgCollection.addSeries(allTagsNdcgSeries);
			precisionCollection.addSeries(allTagsPrecisionSeries);

			// Compute one per AK tag.
			for (String tag : ReportGen.POSITIVE_TAGS) {
				PrecisionAnalyzer tagPrecisionAnalyzer = new PrecisionAnalyzer(ds, Collections.singleton(tag), ReportGen.NEGATIVE_TAGS, MAX_RESULTS);
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

	private void savePrecisionCSV(Path file, PrecisionResult result) {
		AnalysisUtils.writeCSV(file, new String[]{"N", "NDCG", "PRECISION"}, printer -> {
			for (int i = 0; i < result.ndcg().length; i++) {
				int n = i + 1;
				printer.printRecord(n, result.ndcg()[i], result.precision()[i]);
			}
		});
	}
}
