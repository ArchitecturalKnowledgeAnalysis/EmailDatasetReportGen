package nl.andrewl.emaildatasetreportgen.pattern;

import java.util.*;

public record CoOccurrenceSearchResult (
		Map<Set<String>, PatternResult> resultMap
) {
	public CoOccurrenceSearchResult(Collection<Set<String>> patterns) {
		this(new HashMap<>());
		for (Set<String> pattern : patterns) {
			resultMap.put(pattern, new PatternResult(pattern, new ArrayList<>()));
		}
	}

	public Set<Set<String>> patterns() {
		return resultMap.keySet();
	}

	public List<PatternResult> resultsOrdered() {
		return resultMap.values().stream()
				.sorted(Comparator.comparing(PatternResult::count).reversed())
				.toList();
	}

	public record PatternResult (Set<String> pattern, List<Long> emailIds) {
		public int count() {
			return emailIds.size();
		}
	}
}
