package nl.andrewl.emaildatasetreportgen.cmd;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailEntryPreview;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.emaildatasetreportgen.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Generates characteristic report data.
 */
public class CharacteristicReportGenerator implements ReportGenerator {
	private record SizeData (List<Integer> akSizes, List<Integer> notAkSizes) {
		public SizeData() {
			this(new ArrayList<>(), new ArrayList<>());
		}
	}

	private record SizeDataSet (SizeData anyTagData, Map<String, SizeData> tagData) {
		public SizeDataSet() {
			this(new SizeData(), new HashMap<>());
		}
	}

	@Override
	public void generate(Path outputPath, EmailDataset ds) throws Exception {
		Path myDir = outputPath.resolve("characteristics");
		Files.createDirectory(myDir);
		var emailRepo = new EmailRepository(ds);
		var tagRepo = new TagRepository(ds);

		var emailSizeData = getIndividualEmailSizeData(ds, tagRepo);
		generateBoxplot(emailSizeData, "Email Body Size", myDir.resolve("body_size.png"), "Email Body Size (# of characters)");

		var wordCountData = getIndividualEmailWordCountData(ds, tagRepo);
		generateBoxplot(wordCountData, "Email Word Count", myDir.resolve("word_count.png"), "Email Word Count (# of words)");

		var threadSizeData = getEmailThreadSizeData(ds, emailRepo, tagRepo);
		generateBoxplot(threadSizeData, "Email Thread Size", myDir.resolve("thread_size.png"), "Email Thread Size (# of emails)");

		var participantSizeData = getEmailParticipantCountData(ds, emailRepo, tagRepo);
		generateBoxplot(participantSizeData, "Email Thread Participation", myDir.resolve("thread_participation.png"), "Email Thread Participation (# of participants)");
	}

	private SizeDataSet getIndividualEmailSizeData(EmailDataset ds, TagRepository tagRepo) {
		SizeData anyTagSizeData = new SizeData();
		Map<String, SizeData> tagSizeData = new HashMap<>();

		AnalysisUtils.doForAllEmails(
				ds,
				Filters.taggedEmails(tagRepo),
				(email, tags) -> {
					int bodyLength = email.body().length();
					AkStatus statusAnyTag = AnalysisUtils.getStatus(email.id(), tagRepo, ReportGen.POSITIVE_TAGS, ReportGen.NEGATIVE_TAGS);
					switch (statusAnyTag) {
						case ARCHITECTURAL -> anyTagSizeData.akSizes.add(bodyLength);
						case NOT_ARCHITECTURAL -> anyTagSizeData.notAkSizes.add(bodyLength);
					}

					for (String tag : ReportGen.POSITIVE_TAGS) {
						AkStatus tagStatus = AnalysisUtils.getStatus(email.id(), tagRepo, Collections.singleton(tag), ReportGen.NEGATIVE_TAGS);
						SizeData sizeData = tagSizeData.computeIfAbsent(tag, t -> new SizeData());
						switch (tagStatus) {
							case ARCHITECTURAL -> sizeData.akSizes.add(bodyLength);
							case NOT_ARCHITECTURAL -> sizeData.notAkSizes.add(bodyLength);
						}
					}
				}
		);
		return new SizeDataSet(anyTagSizeData, tagSizeData);
	}

	private SizeDataSet getIndividualEmailWordCountData(EmailDataset ds, TagRepository tagRepo) {
		SizeDataSet data = new SizeDataSet();
		AnalysisUtils.doForAllEmails(
				ds,
				Filters.taggedEmails(tagRepo),
				(email, tags) -> {
					int wordCount = email.body().split("\\s+").length;
					AkStatus statusAnyTag = AnalysisUtils.getStatus(email.id(), tagRepo, ReportGen.POSITIVE_TAGS, ReportGen.NEGATIVE_TAGS);
					switch (statusAnyTag) {
						case ARCHITECTURAL -> data.anyTagData.akSizes.add(wordCount);
						case NOT_ARCHITECTURAL -> data.anyTagData.notAkSizes.add(wordCount);
					}

					for (String tag : ReportGen.POSITIVE_TAGS) {
						AkStatus tagStatus = AnalysisUtils.getStatus(email.id(), tagRepo, Collections.singleton(tag), ReportGen.NEGATIVE_TAGS);
						SizeData sizeData = data.tagData.computeIfAbsent(tag, t -> new SizeData());
						switch (tagStatus) {
							case ARCHITECTURAL -> sizeData.akSizes.add(wordCount);
							case NOT_ARCHITECTURAL -> sizeData.notAkSizes.add(wordCount);
						}
					}
				}
		);
		return data;
	}

	private SizeDataSet getEmailThreadSizeData(EmailDataset ds, EmailRepository emailRepo, TagRepository tagRepo) {
		SizeData anyTagSizeData = new SizeData();
		Map<String, SizeData> tagSizeData = new HashMap<>();
		AnalysisUtils.doForAllEmails(
				ds,
				Filters.taggedThreads(tagRepo),
				(email, tags) -> {
					int threadSize = (int) emailRepo.countRepliesRecursive(email.id());
					AkStatus statusAnyTag = AnalysisUtils.getStatus(email.id(), tagRepo, ReportGen.POSITIVE_TAGS, ReportGen.NEGATIVE_TAGS);
					switch (statusAnyTag) {
						case ARCHITECTURAL -> anyTagSizeData.akSizes.add(threadSize);
						case NOT_ARCHITECTURAL -> anyTagSizeData.notAkSizes.add(threadSize);
					}

					for (String tag : ReportGen.POSITIVE_TAGS) {
						AkStatus tagStatus = AnalysisUtils.getStatus(email.id(), tagRepo, Collections.singleton(tag), ReportGen.NEGATIVE_TAGS);
						SizeData sizeData = tagSizeData.computeIfAbsent(tag, t -> new SizeData());
						switch (tagStatus) {
							case ARCHITECTURAL -> sizeData.akSizes.add(threadSize);
							case NOT_ARCHITECTURAL -> sizeData.notAkSizes.add(threadSize);
						}
					}
				}
		);
		return new SizeDataSet(anyTagSizeData, tagSizeData);
	}

	private SizeDataSet getEmailParticipantCountData(EmailDataset ds, EmailRepository emailRepo, TagRepository tagRepo) {
		SizeDataSet data = new SizeDataSet();
		AnalysisUtils.doForAllEmails(
				ds,
				Filters.taggedThreads(tagRepo),
				(email, tags) -> {
					int participantCount = getUniqueParticipantCount(email.id(), emailRepo);
					AkStatus statusAnyTag = AnalysisUtils.getStatus(email.id(), tagRepo, ReportGen.POSITIVE_TAGS, ReportGen.NEGATIVE_TAGS);
					switch (statusAnyTag) {
						case ARCHITECTURAL -> data.anyTagData.akSizes.add(participantCount);
						case NOT_ARCHITECTURAL -> data.anyTagData.notAkSizes.add(participantCount);
					}

					for (String tag : ReportGen.POSITIVE_TAGS) {
						AkStatus tagStatus = AnalysisUtils.getStatus(email.id(), tagRepo, Collections.singleton(tag), ReportGen.NEGATIVE_TAGS);
						SizeData sizeData = data.tagData.computeIfAbsent(tag, t -> new SizeData());
						switch (tagStatus) {
							case ARCHITECTURAL -> sizeData.akSizes.add(participantCount);
							case NOT_ARCHITECTURAL -> sizeData.notAkSizes.add(participantCount);
						}
					}
				}
		);
		return data;
	}

	private int getUniqueParticipantCount(long rootId, EmailRepository emailRepo) {
		Set<String> participants = new HashSet<>();
		Queue<Long> idsToCheck = new LinkedList<>();
		idsToCheck.add(rootId);
		while (!idsToCheck.isEmpty()) {
			long nextId = idsToCheck.remove();
			EmailEntryPreview prev = emailRepo.findPreviewById(nextId).orElseThrow();
			participants.add(prev.sentFrom());
			emailRepo.findAllReplies(nextId).forEach(reply -> idsToCheck.add(reply.id()));
		}
		return participants.size();
	}

	private void generateBoxplot(SizeDataSet dataSet, String title, Path file, String valueAxisLabel) throws IOException {
		DefaultBoxAndWhiskerCategoryDataset data = new DefaultBoxAndWhiskerCategoryDataset();
		data.add(dataSet.anyTagData.akSizes, "Architectural", "Any Tag");
		data.add(dataSet.anyTagData.notAkSizes, "Non-architectural", "Any Tag");

		List<String> tags = dataSet.tagData.keySet().stream().sorted().toList();
		for (var tag : tags) {
			var tagData = dataSet.tagData.get(tag);
			data.add(tagData.akSizes, "Architectural", tag);
			data.add(tagData.notAkSizes, "Non-architectural", tag);
		}

		JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(
				title,
				"Type",
				valueAxisLabel,
				data,
				true
		);
		CategoryPlot plot = chart.getCategoryPlot();
		plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		ChartUtils.saveChartAsPNG(file.toFile(), chart, 1000, 500);
	}
}
