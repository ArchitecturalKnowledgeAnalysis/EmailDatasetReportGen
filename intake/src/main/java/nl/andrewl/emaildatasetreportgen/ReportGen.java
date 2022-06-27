package nl.andrewl.emaildatasetreportgen;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.emaildatasetreportgen.cmd.*;
import nl.andrewl.emaildatasetreportgen.relevance.RelevanceAnalyzer;
import org.jfree.chart.ChartColor;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.resources.JFreeChartResources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ReportGen {
	public static final List<String> POSITIVE_TAGS = List.of("technology", "process", "existence", "existence-structural", "existence-behavioral", "property");
	public static final List<String> NEGATIVE_TAGS = List.of("not-ak");

	public static void main(String[] args) throws Exception {
		Path datasetPath = Path.of(args[0]);
		EmailDataset ds = EmailDataset.open(datasetPath).join();
		LocalDateTime now = LocalDateTime.now();
		String reportDirName = String.format(
				"report_%04d-%02d-%02d_%02d-%02d-%02d",
				now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour(), now.getMinute(), now.getSecond()
		);
		Path outputDir = Path.of(".", reportDirName);
		Files.createDirectory(outputDir);

		// Pre-compute max-ak-count which is needed for relevance calculations in multiple reports.
//		System.out.println("Pre-computing maximum AK tag count for threads, for relevance calculations.");
//		double maxAkCount = RelevanceAnalyzer.getMaxAkCount(ds, POSITIVE_TAGS);
//		System.out.println("Maximum AK tag count: " + maxAkCount);
//		RelevanceAnalyzer relevanceAnalyzer = new RelevanceAnalyzer(new EmailRepository(ds), new TagRepository(ds), POSITIVE_TAGS, maxAkCount);

		// Run all reports.
		new JsonDataGenerator().generate(outputDir, ds);
//		new OverviewReportGenerator(relevanceAnalyzer).generate(outputDir, ds);
//		new PrecisionReportGenerator(relevanceAnalyzer).generate(outputDir, ds);
//		new CharacteristicReportGenerator().generate(outputDir, ds);
//		new PatternReportGenerator().generate(outputDir, ds);

		ds.close().join();
		System.out.println("All reports completed.");

		// Save the results to a ZIP for convenience.
//		saveToZIP(reportDirName, outputDir);
	}

	private static void saveToZIP(String reportDirName, Path outputDir) throws IOException {
		System.out.println("Saving to ZIP.");
		ZipFile zip = new ZipFile(reportDirName + ".zip");
		ZipParameters params = new ZipParameters();
		params.setCompressionLevel(CompressionLevel.ULTRA);
		try (var s = Files.list(outputDir)) {
			for (var p : s.toList()) {
				if (Files.isDirectory(p)) {
					zip.addFolder(p.toFile(), params);
				} else if (Files.isRegularFile(p)) {
					zip.addFile(p.toFile(), params);
				}
			}
		}
		zip.close();
		System.out.println("Done.");
	}

	public static Map<String, String> getQueries() throws IOException {
		Properties props = new Properties();
		props.load(ReportGen.class.getClassLoader().getResourceAsStream("queries.properties"));
		Map<String, String> queries = new HashMap<>();
		for (var entry : props.entrySet()) {
			queries.put((String) entry.getKey(), (String) entry.getValue());
		}
		return queries;
	}
}
