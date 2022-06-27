package nl.andrewl.emaildatasetreportgen;

import nl.andrewl.email_indexer.data.EmailEntry;
import nl.andrewl.email_indexer.data.Tag;

import java.util.Collection;

@FunctionalInterface
public interface DatasetEmailConsumer {
	void consumeEmail(EmailEntry email, Collection<Tag> tags) throws Exception;
}
