package nl.andrewl.jvisualizer.render;

import com.google.gson.JsonObject;
import nl.andrewl.jvisualizer.ChartRenderer;
import nl.andrewl.jvisualizer.JVisualizer;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class PatternRenderer implements ChartRenderer {
	@Override
	public void renderCharts(JsonObject data) throws Exception {
		Path dir = Path.of("patterns");
		Files.createDirectory(dir);
		exportNGramData(dir.resolve("ngram_2_no-skip.txt"), data.getAsJsonObject("ngram_2_no-skip"), JVisualizer.AK_TAGS);
		exportNGramData(dir.resolve("ngram_2_skip.txt"), data.getAsJsonObject("ngram_2_skip"), JVisualizer.AK_TAGS);
		exportNGramData(dir.resolve("ngram_3_no-skip.txt"), data.getAsJsonObject("ngram_3_no-skip"), JVisualizer.AK_TAGS);
		exportNGramData(dir.resolve("ngram_3_skip.txt"), data.getAsJsonObject("ngram_3_skip"), JVisualizer.AK_TAGS);
		exportNGramData(dir.resolve("co-occurrence_2.txt"), data.getAsJsonObject("co-occurrence_2"), JVisualizer.AK_TAGS);
		exportNGramData(dir.resolve("co-occurrence_3.txt"), data.getAsJsonObject("co-occurrence_3"), JVisualizer.AK_TAGS);
	}

	private void exportNGramData(Path file, JsonObject data, Collection<String> eligibleTags) throws IOException {
		Map<List<String>, Integer> patternCounts = new HashMap<>();
		int longestTagLength = -1;
		for (var entry : data.entrySet()) {
			JsonObject patternData = entry.getValue().getAsJsonObject();
			int count = patternData.get("count").getAsInt();
			List<String> tags = new ArrayList<>();
			boolean shouldSkip = false; // Check if we should skip this entry based on if its pattern contains ineligible tags.
			for (var tagJson : patternData.get("pattern").getAsJsonArray()) {
				String tag = tagJson.getAsString().trim();
				if (!eligibleTags.contains(tag)) {
					shouldSkip = true;
					break;
				}
				longestTagLength = Math.max(longestTagLength, tag.length());
				tags.add(tag);
			}
			if (!shouldSkip) {
				patternCounts.put(tags, count);
			}
		}

		var sortedPatternCounts = new ArrayList<>(patternCounts.entrySet().stream()
				.sorted(Comparator.comparingInt(Map.Entry::getValue)).toList());
		Collections.reverse(sortedPatternCounts);

		int paddingSize = longestTagLength + 1;

		try (var out = new PrintWriter(Files.newBufferedWriter(file))) {
			for (var entry : sortedPatternCounts) {
				var tags = entry.getKey();
				int count = entry.getValue();
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < tags.size(); i++) {
					String tag = tags.get(i);
					sb.append(tag).append(" ".repeat(paddingSize - tag.length()));
					if (i < tags.size() - 1) {
						sb.append(" $ \\rightarrow $ ");
					}
				}
				out.print(sb);
				out.print(" & ");
				out.print(count);
				out.print(" \\\\ \\hline\n");
			}
		}
	}
}
