package nl.andrewl.emaildatasetreportgen;

import nl.andrewl.email_indexer.data.Tag;
import nl.andrewl.email_indexer.data.TagRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AnalysisUtils {
	public static final List<String> TAGS_AK = List.of("technology", "process", "ban", "existence", "existence-structural", "existence-behavioral", "property");
	public static final List<String> TAGS_NOT_AK = List.of("not-ak");

	public static AkStatus getStatus(long id, TagRepository tagRepo) {
		List<Tag> tags = tagRepo.getTags(id);
		if (tags.stream().anyMatch(t -> TAGS_AK.contains(t.name().toLowerCase()))) {
			return AkStatus.ARCHITECTURAL;
		} else if (tags.stream().anyMatch(t -> TAGS_NOT_AK.contains(t.name().toLowerCase()))) {
			return AkStatus.NOT_ARCHITECTURAL;
		} else {
			return AkStatus.UNKNOWN;
		}
	}

	public static double discountedCumulativeGain(List<Double> values) {
		double sum = 0;
		for (int i = 1; i <= values.size(); i++) {
			sum += (values.get(i - 1) / log2(i + 1));
		}
		return sum;
	}

	public static double normalizedDiscountedCumulativeGain(List<Double> values) {
		List<Double> sortedValues = new ArrayList<>(values);
		sortedValues.sort(Collections.reverseOrder());
		double realGain = discountedCumulativeGain(values);
		double idealGain = discountedCumulativeGain(sortedValues);
		return realGain / idealGain;
	}

	public static double log2(double n) {
		return Math.log(n) / Math.log(2);
	}
}
