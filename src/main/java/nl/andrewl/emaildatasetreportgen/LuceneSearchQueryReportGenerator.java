package nl.andrewl.emaildatasetreportgen;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.search.EmailIndexSearcher;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Objects;

public class LuceneSearchQueryReportGenerator extends HtmlReportGenerator {
	public record Settings (
			String query,
			int resultCount
	) {}

	private final Settings settings;

	public LuceneSearchQueryReportGenerator(Settings settings) {
		super("lucene_report");
		this.settings = settings;
	}

	@Override
	protected void generate(Context ctx, EmailDataset ds) throws Exception {
		List<String> allIds = new EmailIndexSearcher().search(ds, settings.query);
		var repo = new EmailRepository(ds);
		List<EmailData> emails = allIds.subList(0, Math.min(settings.resultCount, allIds.size()))
				.stream()
				.map(id -> EmailData.fromMessageId(id, repo))
				.filter(Objects::nonNull)
				.toList();
		ctx.setVariable("settings", settings);
		ctx.setVariable("emails", emails);
	}
}
