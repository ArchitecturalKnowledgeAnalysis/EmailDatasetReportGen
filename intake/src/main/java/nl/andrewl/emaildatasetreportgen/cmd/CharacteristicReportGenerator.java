package nl.andrewl.emaildatasetreportgen.cmd;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailEntryPreview;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.email_indexer.data.search.SearchFilter;
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
	private record SizeDataSet (List<Integer> anyTagData, List<Integer> notAkData, Map<String, List<Integer>> tagData) {
		public SizeDataSet() {
			this(new ArrayList<>(), new ArrayList<>(), new HashMap<>());
		}

		public void addTagSizeData(String tag, int size) {
			tagData.computeIfAbsent(tag, t -> new ArrayList<>()).add(size);
		}
	}

	@Override
	public void generate(Path outputPath, EmailDataset ds) throws Exception {
		Path myDir = outputPath.resolve("characteristics");
		Files.createDirectory(myDir);

		System.out.println("Generating characteristic data.");

		var emailSizeData = getSizeData(ds, (email, emailRepo, tagRepo) -> email.body().length(), false);
		generateBoxplot(emailSizeData, "Email Body Size", myDir.resolve("body_size.png"), "Email Body Size (# of characters)");

		var wordCountData = getSizeData(ds, (email, emailRepo, tagRepo) -> email.body().split("\\s+").length, false);
		generateBoxplot(wordCountData, "Email Word Count", myDir.resolve("word_count.png"), "Email Word Count (# of words)");

		var threadSizeData = getSizeData(ds, (email, emailRepo, tagRepo) -> (int) emailRepo.countRepliesRecursive(email.id()) + 1, true);
		generateBoxplot(threadSizeData, "Email Thread Size", myDir.resolve("thread_size.png"), "Email Thread Size (# of emails)");

		var participantSizeData = getSizeData(ds, (email, emailRepo, tagRepo) -> getUniqueParticipantCount(email.id(), emailRepo), true);
		generateBoxplot(participantSizeData, "Email Thread Participation", myDir.resolve("thread_participation.png"), "Email Thread Participation (# of participants)");

		Map<String, SizeDataSet> dataSets = Map.of(
				"Email Body Size", emailSizeData,
				"Email Word Count", wordCountData,
				"Email Thread Size", threadSizeData,
				"Email Thread Participation", participantSizeData
		);

		Set<String> allTags = new HashSet<>();
		for (var dataset : dataSets.values()) allTags.addAll(dataset.tagData.keySet());
		List<String> tagsOrdered = allTags.stream().sorted().toList();

		List<String> headers = new ArrayList<>();
		headers.add("CHARACTERISTIC");
		headers.add("NOT_AK");
		headers.add("ANY_TAG");
		headers.addAll(tagsOrdered);
		AnalysisUtils.writeCSV(myDir.resolve("data.csv"), headers, printer -> {
			for (var entry : dataSets.entrySet()) {
				SizeDataSet dataSet = entry.getValue();
				printer.print(entry.getKey());
				printer.print(dataSet.notAkData);
				printer.print(dataSet.anyTagData);
				for (String tag : tagsOrdered) {
					if (dataSet.tagData.containsKey(tag)) {
						printer.print(dataSet.tagData.get(tag));
					} else {
						printer.print(Collections.emptyList());
					}
				}
				printer.println();
			}
		});
	}

	private SizeDataSet getSizeData(EmailDataset ds, EmailCharacteristic characteristic, boolean threaded) {
		SizeDataSet sizeData = new SizeDataSet();
		var tagRepo = new TagRepository(ds);
		var emailRepo = new EmailRepository(ds);
		Collection<SearchFilter> filters = threaded ? Filters.taggedThreads(tagRepo) : Filters.taggedEmails(tagRepo);
		AnalysisUtils.doForAllEmails(
				ds,
				filters,
				(email, tags) -> {
					int v = characteristic.getCharacteristic(email, emailRepo, tagRepo);
					AkStatus statusAnyTag = AnalysisUtils.getStatus(email.id(), tagRepo, ReportGen.POSITIVE_TAGS, ReportGen.NEGATIVE_TAGS);
					switch (statusAnyTag) {
						case ARCHITECTURAL -> sizeData.anyTagData.add(v);
						case NOT_ARCHITECTURAL -> sizeData.notAkData.add(v);
					}

					for (String tag : ReportGen.POSITIVE_TAGS) {
						AkStatus tagStatus = AnalysisUtils.getStatus(email.id(), tagRepo, Collections.singleton(tag), ReportGen.NEGATIVE_TAGS);
						if (tagStatus == AkStatus.ARCHITECTURAL) sizeData.addTagSizeData(tag, v);
					}
				}
		);
		return sizeData;
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
		data.add(dataSet.anyTagData, "Architectural", "Any Tag");
		data.add(dataSet.notAkData, "Non-architectural", "Any Tag");

		List<String> tags = dataSet.tagData.keySet().stream().sorted().toList();
		for (var tag : tags) {
			var tagData = dataSet.tagData.get(tag);
			data.add(tagData, "Architectural", tag);
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
