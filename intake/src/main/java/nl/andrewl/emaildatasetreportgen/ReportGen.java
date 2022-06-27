package nl.andrewl.emaildatasetreportgen;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.emaildatasetreportgen.cmd.JsonDataGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ReportGen {
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
		new JsonDataGenerator().generate(outputDir, ds);
		ds.close().join();
		System.out.println("All reports completed.");
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
