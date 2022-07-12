package nl.andrewl.emaildatasetreportgen;

import nl.andrewl.email_indexer.data.Tag;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.email_indexer.data.search.SearchFilter;
import nl.andrewl.email_indexer.data.search.filter.HiddenFilter;
import nl.andrewl.email_indexer.data.search.filter.TagFilter;

import java.util.Collection;
import java.util.Set;

public final class Filters {
	public static Collection<SearchFilter> taggedEmails(TagRepository tagRepo) {
		return Set.of(
				new HiddenFilter(false),
				new TagFilter(tagRepo.findAll().stream().map(Tag::id).toList(), TagFilter.Type.INCLUDE_ANY)
		);
	}

	public static Collection<SearchFilter> taggedEmails(TagRepository tagRepo, Set<String> tags) {
		return Set.of(
				new HiddenFilter(false),
				new TagFilter(tagRepo.findAll().stream().filter(t -> tags.contains(t.name())).map(Tag::id).toList(), TagFilter.Type.INCLUDE_ANY)
		);
	}
}
