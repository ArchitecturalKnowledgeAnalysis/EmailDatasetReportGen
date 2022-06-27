package nl.andrewl.emaildatasetreportgen.cmd;

import nl.andrewl.email_indexer.data.EmailEntry;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.TagRepository;

public interface EmailCharacteristic {
	int getCharacteristic(EmailEntry email, EmailRepository emailRepo, TagRepository tagRepo);
}
