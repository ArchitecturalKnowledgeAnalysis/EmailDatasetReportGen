package nl.andrewl.emaildatasetreportgen;

import nl.andrewl.email_indexer.data.EmailDataset;

import java.nio.file.Path;

public interface ReportGenerator {
	 void generate(Path outputPath, EmailDataset ds) throws Exception;
}
