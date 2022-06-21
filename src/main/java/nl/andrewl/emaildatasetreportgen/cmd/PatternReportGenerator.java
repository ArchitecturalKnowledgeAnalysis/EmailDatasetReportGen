package nl.andrewl.emaildatasetreportgen.cmd;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.emaildatasetreportgen.AnalysisUtils;
import nl.andrewl.emaildatasetreportgen.Filters;
import nl.andrewl.emaildatasetreportgen.ReportGen;
import nl.andrewl.emaildatasetreportgen.ReportGenerator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PatternReportGenerator implements ReportGenerator {
	@Override
	public void generate(Path outputPath, EmailDataset ds) throws Exception {
		Path myDir = outputPath.resolve("patterns");
		Files.createDirectory(myDir);

		System.out.println("Generating pattern data.");
		writeNGramResults(myDir.resolve("2gram_no-skip.csv"), doNGramAnalysis(2, ds, false));
		writeNGramResults(myDir.resolve("2gram_skip.csv"), doNGramAnalysis(2, ds, true));
		writeNGramResults(myDir.resolve("3gram_no-skip.csv"), doNGramAnalysis(3, ds, false));
		writeNGramResults(myDir.resolve("3gram_skip.csv"), doNGramAnalysis(3, ds, true));
	}

	private List<List<String>> generateTagPermutations(int size, List<String> tags) {
		if (size <= 0) return new ArrayList<>();
		List<List<String>> permutations = new ArrayList<>();
		if (size == 1) {// Return a list of permutations that's just a list of singletons.
			for (String tag : tags) {
				List<String> singleton = new ArrayList<>(1);
				singleton.add(tag);
				permutations.add(singleton);
			}
		} else {
			List<List<String>> firstPermutations = generateTagPermutations(size - 1, tags);
			for (List<String> permutation : firstPermutations) {
				for (String tag : tags) {
					if (permutation.contains(tag)) continue;
					List<String> newPermutation = new ArrayList<>(permutation);
					newPermutation.add(tag);
					permutations.add(newPermutation);
				}
			}
		}
		return permutations;
	}

	private List<NGramPatternSearcher.Result> doNGramAnalysis(int size, EmailDataset ds, boolean skipNotAk) {
		var possiblePatterns = generateTagPermutations(size, ReportGen.POSITIVE_TAGS);
		var emailRepo = new EmailRepository(ds);
		var tagRepo = new TagRepository(ds);
		NGramPatternSearcher searcher = new NGramPatternSearcher(possiblePatterns, emailRepo, tagRepo, skipNotAk);
		AnalysisUtils.doForAllEmails(ds, Filters.taggedThreads(tagRepo), searcher);
		return searcher.getResultsOrdered();
	}

	private void writeNGramResults(Path filename, List<NGramPatternSearcher.Result> results) {
		AnalysisUtils.writeCSV(filename, new String[]{"RANK", "PATTERN", "COUNT"}, printer -> {
			for (int i = 0; i < results.size(); i++) {
				var result = results.get(i);
				printer.printRecord(i + 1, result.pattern(), result.count());
			}
		});
	}
}
