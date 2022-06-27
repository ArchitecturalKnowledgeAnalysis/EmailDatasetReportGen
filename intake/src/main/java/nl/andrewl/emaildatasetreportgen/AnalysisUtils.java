package nl.andrewl.emaildatasetreportgen;

import nl.andrewl.email_indexer.data.*;
import nl.andrewl.email_indexer.data.search.EmailSearchResult;
import nl.andrewl.email_indexer.data.search.EmailSearcher;
import nl.andrewl.email_indexer.data.search.SearchFilter;

import java.util.Collection;
import java.util.List;

public final class AnalysisUtils {
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
}
