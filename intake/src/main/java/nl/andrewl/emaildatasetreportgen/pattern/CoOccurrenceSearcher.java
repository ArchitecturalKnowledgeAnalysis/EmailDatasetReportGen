package nl.andrewl.emaildatasetreportgen.pattern;

import nl.andrewl.email_indexer.data.EmailEntry;
import nl.andrewl.email_indexer.data.Tag;
import nl.andrewl.emaildatasetreportgen.DatasetEmailConsumer;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class CoOccurrenceSearcher implements DatasetEmailConsumer {
	private final CoOccurrenceSearchResult result;

	public CoOccurrenceSearcher(Collection<Set<String>> patterns) {
		this.result = new CoOccurrenceSearchResult(patterns);
	}

	public CoOccurrenceSearchResult getResult() {
		return result;
	}

	@Override
	public void consumeEmail(EmailEntry email, Collection<Tag> tags) {
		Set<String> tagNames = tags.stream().map(Tag::name).collect(Collectors.toSet());
		for (Set<String> pattern : result.resultMap().keySet()) {
			if (tagNames.containsAll(pattern)) {
				result.resultMap().get(pattern).emailIds().add(email.id());
			}
		}
	}
}
