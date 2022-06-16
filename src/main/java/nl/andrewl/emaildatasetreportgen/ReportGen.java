package nl.andrewl.emaildatasetreportgen;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.emaildatasetreportgen.cmd.AnalyzeQuery;
import nl.andrewl.emaildatasetreportgen.cmd.TopThreads;
import picocli.CommandLine;

import java.nio.file.Path;

@CommandLine.Command(
		name = "reportgen",
		description = "Analyze and generate reports for email datasets.",
		mixinStandardHelpOptions = true,
		subcommands = {
				TopThreads.class,
				AnalyzeQuery.class
		}
)
public class ReportGen {
	@CommandLine.Parameters(index = "0", description = "The path to the dataset to analyze.")
	private Path datasetPath;

	public EmailDataset getDataset() {
		return EmailDataset.open(datasetPath).join();
	}

	public static void main(String[] args) {
		int code = new CommandLine(new ReportGen()).execute(args);
		System.exit(code);
	}
}
