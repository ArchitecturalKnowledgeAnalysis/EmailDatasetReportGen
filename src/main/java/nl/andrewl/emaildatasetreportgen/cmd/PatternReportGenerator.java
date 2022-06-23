package nl.andrewl.emaildatasetreportgen.cmd;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.emaildatasetreportgen.AnalysisUtils;
import nl.andrewl.emaildatasetreportgen.Filters;
import nl.andrewl.emaildatasetreportgen.ReportGen;
import nl.andrewl.emaildatasetreportgen.ReportGenerator;
import nl.andrewl.emaildatasetreportgen.pattern.CoOccurrenceSearchResult;
import nl.andrewl.emaildatasetreportgen.pattern.CoOccurrenceSearcher;
import nl.andrewl.emaildatasetreportgen.pattern.NGramPatternSearcher;
import nl.andrewl.emaildatasetreportgen.pattern.NGramSearchResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PatternReportGenerator implements ReportGenerator {
	@Override
	public void generate(Path outputPath, EmailDataset ds) throws Exception {
		Path myDir = outputPath.resolve("patterns");
		Files.createDirectory(myDir);

		System.out.println("Generating pattern data.");

		System.out.println("Doing 2gram analysis.");
		doNGramAnalysis(2, ds, false, myDir.resolve("2gram_no-skip.csv"));
		doNGramAnalysis(2, ds, true, myDir.resolve("2gram_skip.csv"));
		System.out.println("Doing 3gram analysis.");
		doNGramAnalysis(3, ds, false, myDir.resolve("3gram_no-skip.csv"));
		doNGramAnalysis(3, ds, true, myDir.resolve("3gram_skip.csv"));

		System.out.println("Doing 2-co-occurrence analysis.");
		doCoOccurrenceAnalysis(2, ds, myDir.resolve("2co_occurrence.csv"));
		doCoOccurrenceAnalysis(3, ds, myDir.resolve("3co_occurrence.csv"));
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

	private void doNGramAnalysis(int size, EmailDataset ds, boolean skipNotAk, Path outFile) {
		List<String> tags = new ArrayList<>(ReportGen.POSITIVE_TAGS);
		tags.remove("existence-structural");
		tags.remove("existence-behavioral");
		var possiblePatterns = generateTagPermutations(size, tags);
		var emailRepo = new EmailRepository(ds);
		var tagRepo = new TagRepository(ds);
		NGramPatternSearcher searcher = new NGramPatternSearcher(possiblePatterns, emailRepo, tagRepo, skipNotAk);
		AnalysisUtils.doForAllEmails(ds, Filters.taggedThreads(tagRepo), searcher);
		writeNGramResults(outFile, searcher.getResult());
	}

	private void writeNGramResults(Path filename, NGramSearchResult result) {
		AnalysisUtils.writeCSV(filename, new String[]{"RANK", "PATTERN", "COUNT", "SEQUENCES"}, printer -> {
			List<NGramSearchResult.PatternResult> results = result.resultsOrdered();
			for (int i = 0; i < results.size(); i++) {
				var r = results.get(i);
				printer.printRecord(i + 1, r.pattern(), r.count(), r.emailIdSequences());
			}
		});
	}

	private void doCoOccurrenceAnalysis(int size, EmailDataset ds, Path outFile) {
		List<String> tags = new ArrayList<>(ReportGen.POSITIVE_TAGS);
		tags.remove("existence-structural");
		tags.remove("existence-behavioral");
		List<Set<String>> patterns = generateTagPermutations(size, tags).stream()
				.map(Set::copyOf).toList();
		var tagRepo = new TagRepository(ds);
		CoOccurrenceSearcher searcher = new CoOccurrenceSearcher(patterns);
		AnalysisUtils.doForAllEmails(ds, Filters.taggedEmails(tagRepo), searcher);
		writeCoOccurrenceResults(outFile, searcher.getResult());
	}

	private void writeCoOccurrenceResults(Path outFile, CoOccurrenceSearchResult result) {
		List<CoOccurrenceSearchResult.PatternResult> results = result.resultsOrdered();
		AnalysisUtils.writeCSV(outFile, List.of("RANK", "PATTERN", "COUNT", "EMAIL_IDS"), printer -> {
			for (int i = 0; i < results.size(); i++) {
				var r = results.get(i);
				printer.printRecord(
						i + 1,
						r.pattern().stream().sorted().toList(),
						r.count(), r.emailIds().stream().sorted().toList()
				);
			}
		});
	}
}
