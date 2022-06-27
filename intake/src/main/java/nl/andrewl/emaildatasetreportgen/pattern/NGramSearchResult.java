package nl.andrewl.emaildatasetreportgen.pattern;

import java.util.*;

public record NGramSearchResult(
		Map<List<String>, PatternResult> resultMap
) {
	public NGramSearchResult(List<List<String>> patterns) {
		this(
				new HashMap<>()
		);
		for (List<String> pattern : patterns) {
			resultMap.put(pattern, new PatternResult(pattern, new ArrayList<>()));
		}
	}

	public Set<List<String>> patterns() {
		return resultMap.keySet();
	}

	public List<PatternResult> resultsOrdered() {
		return resultMap.values().stream()
				.sorted(Comparator.comparing(PatternResult::count).reversed())
				.toList();
	}

	public record PatternResult(List<String> pattern, List<List<Long>> emailIdSequences) {
		public int count() {
			return emailIdSequences.size();
		}
	}
}
