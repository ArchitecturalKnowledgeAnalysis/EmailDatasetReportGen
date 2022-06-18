package nl.andrewl.emaildatasetreportgen;

import nl.andrewl.email_indexer.data.*;
import nl.andrewl.email_indexer.data.search.EmailSearchResult;
import nl.andrewl.email_indexer.data.search.EmailSearcher;
import nl.andrewl.email_indexer.data.search.SearchFilter;

import java.util.*;

public final class AnalysisUtils {
	public static AkStatus getStatus(long id, TagRepository tagRepo, Collection<String> positiveTags, Collection<String> negativeTags) {
		List<Tag> tags = tagRepo.getTags(id);
		if (tags.stream().anyMatch(t -> positiveTags.contains(t.name().toLowerCase()))) {
			return AkStatus.ARCHITECTURAL;
		} else if (tags.stream().anyMatch(t -> negativeTags.contains(t.name().toLowerCase()))) {
			return AkStatus.NOT_ARCHITECTURAL;
		} else {
			return AkStatus.UNKNOWN;
		}
	}

	public static void doForAllEmails(EmailDataset ds, Collection<SearchFilter> filters, DatasetEmailConsumer consumer) {
		var searcher = new EmailSearcher(ds);
		var emailRepo = new EmailRepository(ds);
		var tagRepo = new TagRepository(ds);
		int page = 1;
		while (true) {
			EmailSearchResult result = searcher.findAll(page++, 1000, filters).join();
			for (var emailPreview : result.emails()) {
				EmailEntry email = emailRepo.findEmailById(emailPreview.id()).orElseThrow();
				List<Tag> tags = tagRepo.getTags(emailPreview.id());
				try {
					consumer.consumeEmail(email, tags);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (!result.hasNextPage()) break;
		}
	}

	public static double discountedCumulativeGain(double[] values) {
		double sum = 0;
		for (int i = 1; i <= values.length; i++) {
			sum += (values[i - 1] / log2(i + 1));
		}
		return sum;
	}

	public static double normalizedDiscountedCumulativeGain(double[] values) {
		double[] sortedValues = new double[values.length];
		System.arraycopy(values, 0, sortedValues, 0, values.length);
		Arrays.sort(sortedValues); // Sort from low to high.
		reverse(sortedValues); // Reverse so we're from high to low.

		double realGain = discountedCumulativeGain(values);
		double idealGain = discountedCumulativeGain(sortedValues);
		return realGain / idealGain;
	}

	public static void reverse(double[] a) {
		for (int i = 0; i < a.length / 2; i++) {
			double tmp = a[i];
			a[i] = a[a.length - i - 1];
			a[a.length - i - 1] = tmp;
		}
	}

	public static double log2(double n) {
		return Math.log(n) / Math.log(2);
	}
}
