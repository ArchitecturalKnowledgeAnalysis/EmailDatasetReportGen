package nl.andrewl.emaildatasetreportgen;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.emaildatasetreportgen.cmd.CharacteristicReportGenerator;
import nl.andrewl.emaildatasetreportgen.cmd.OverviewReportGenerator;
import nl.andrewl.emaildatasetreportgen.cmd.PatternReportGenerator;
import nl.andrewl.emaildatasetreportgen.cmd.PrecisionReportGenerator;

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
		Path datasetPath = Path.of("/home/andrew/Programming/ArchitecturalKnowledgeAnalysis/Thesis/datasets/current");
		EmailDataset ds = EmailDataset.open(datasetPath).join();
		LocalDateTime now = LocalDateTime.now();
		String reportDirName = String.format(
				"report_%04d-%02d-%02d_%02d-%02d-%02d",
				now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour(), now.getMinute(), now.getSecond()
		);
		Path outputDir = Path.of(".", reportDirName);
		Files.createDirectory(outputDir);

		// Run all reports.
		new OverviewReportGenerator().generate(outputDir, ds);
		new PrecisionReportGenerator().generate(outputDir, ds);
		new CharacteristicReportGenerator().generate(outputDir, ds);
		new PatternReportGenerator().generate(outputDir, ds);

		ds.close().join();
		System.out.println("All reports completed.");

		// Save the results to a ZIP for convenience.
		saveToZIP(reportDirName, outputDir);
	}

	private static void saveToZIP(String reportDirName, Path outputDir) throws IOException {
		System.out.println("Saving to ZIP.");
		ZipFile zip = new ZipFile(reportDirName + ".zip");
		ZipParameters params = new ZipParameters();
		params.setCompressionLevel(CompressionLevel.ULTRA);
		try (var s = Files.list(outputDir)) {
			for (var p : s.toList()) zip.addFolder(p.toFile(), params);
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
