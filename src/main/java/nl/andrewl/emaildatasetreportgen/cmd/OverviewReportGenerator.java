package nl.andrewl.emaildatasetreportgen.cmd;

import nl.andrewl.email_indexer.data.EmailDataset;
import nl.andrewl.email_indexer.data.EmailRepository;
import nl.andrewl.email_indexer.data.Tag;
import nl.andrewl.email_indexer.data.TagRepository;
import nl.andrewl.emaildatasetreportgen.AnalysisUtils;
import nl.andrewl.emaildatasetreportgen.Filters;
import nl.andrewl.emaildatasetreportgen.ReportGen;
import nl.andrewl.emaildatasetreportgen.ReportGenerator;
import nl.andrewl.emaildatasetreportgen.relevance.RelevanceAnalyzer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.SimpleHistogramBin;
import org.jfree.data.statistics.SimpleHistogramDataset;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class OverviewReportGenerator implements ReportGenerator {
	@Override
	public void generate(Path outputPath, EmailDataset ds) throws Exception {
		Path myDir = outputPath.resolve("overview");
		Files.createDirectory(myDir);

		System.out.println("Generating overview.");
		Map<String, Integer> emailTagCounts = new HashMap<>();
		Map<String, Integer> threadTagCounts = new HashMap<>();
		List<Double> relevances = new ArrayList<>();
		var emailRepo = new EmailRepository(ds);
		var tagRepo = new TagRepository(ds);
		RelevanceAnalyzer relevanceAnalyzer = new RelevanceAnalyzer(emailRepo, tagRepo, ReportGen.POSITIVE_TAGS, ReportGen.NEGATIVE_TAGS);
		AnalysisUtils.doForAllEmails(ds, Filters.taggedEmails(tagRepo), (email, tags) -> {
			if (email.parentId() == null) {// If the email is the start of a thread.
				if (!tags.isEmpty()) relevances.add(relevanceAnalyzer.analyzeThread(email.id()));
				Set<Tag> threadTags = new HashSet<>();
				threadTags.addAll(tags);
				threadTags.addAll(tagRepo.getAllChildTags(email.id()));
				for (Tag tag : threadTags) {
					int currentCount = threadTagCounts.computeIfAbsent(tag.name(), t -> 0);
					threadTagCounts.put(tag.name(), currentCount + 1);
				}
			} else {// Separate logic for individual emails. This avoids having to do a separate pass.
				for (Tag tag : tags) {
					int currentCount = emailTagCounts.computeIfAbsent(tag.name(), t -> 0);
					emailTagCounts.put(tag.name(), currentCount + 1);
				}
			}
		});

		// Write the CSV.
		List<String> tagNamesOrdered = emailTagCounts.keySet().stream()
				.filter(s -> ReportGen.POSITIVE_TAGS.contains(s) || ReportGen.NEGATIVE_TAGS.contains(s))
				.sorted().toList();
		List<String> headers = new ArrayList<>();
		headers.add("TYPE");
		headers.addAll(tagNamesOrdered);
		AnalysisUtils.writeCSV(myDir.resolve("counts.csv"), headers, printer -> {
			printer.print("Individual Emails");
			for (String tag : tagNamesOrdered) {
				printer.print(emailTagCounts.computeIfAbsent(tag, t -> 0));
			}
			printer.println();
			printer.print("Threads");
			for (String tag : tagNamesOrdered) {
				printer.print(threadTagCounts.computeIfAbsent(tag, t -> 0));
			}
			printer.println();
		});

		// Build the plot
		DefaultCategoryDataset individualEmailDataset = new DefaultCategoryDataset();
		DefaultCategoryDataset threadEmailDataset = new DefaultCategoryDataset();
		for (var entry : emailTagCounts.entrySet()) {
			if (!ReportGen.POSITIVE_TAGS.contains(entry.getKey()) && !ReportGen.NEGATIVE_TAGS.contains(entry.getKey())) continue;
			individualEmailDataset.addValue(entry.getValue(), entry.getKey(), "Email");
		}
		for (var entry : threadTagCounts.entrySet()) {
			if (!ReportGen.POSITIVE_TAGS.contains(entry.getKey()) && !ReportGen.NEGATIVE_TAGS.contains(entry.getKey())) continue;
			threadEmailDataset.addValue(entry.getValue(), entry.getKey(), "Thread");
		}
		JFreeChart chart = ChartFactory.createBarChart("Individual Email Tag Counts", "Tag", "Count", individualEmailDataset);
		CategoryPlot plot = chart.getCategoryPlot();
		BarRenderer renderer = (BarRenderer) plot.getRenderer();
		renderer.setBarPainter(new StandardBarPainter());
		CategoryAxis domainAxis = plot.getDomainAxis();
		domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		ChartUtils.saveChartAsPNG(myDir.resolve("individual_email_tag_count.png").toFile(), chart, 500, 500);

		JFreeChart chart2 = ChartFactory.createBarChart("Thread Tag Counts", "Tag", "Count", threadEmailDataset);
		CategoryPlot plot2 = chart2.getCategoryPlot();
		BarRenderer renderer2 = (BarRenderer) plot2.getRenderer();
		renderer2.setBarPainter(new StandardBarPainter());
		CategoryAxis domainAxis2 = plot2.getDomainAxis();
		domainAxis2.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		ChartUtils.saveChartAsPNG(myDir.resolve("thread_tag_count.png").toFile(), chart2, 500, 500);

		// Write relevance data
		AnalysisUtils.writeCSV(myDir.resolve("relevances.csv"), List.of("RELEVANCE"), printer -> {
			printer.printRecords(relevances);
		});

		SimpleHistogramDataset relevancesDataset = new SimpleHistogramDataset("Relevance");
		double[] relevancesArray = relevances.stream().mapToDouble(v -> v).toArray();
		int binCount = 10;
		double binSize = 1.0 / binCount;
		for (int i = 0; i < binCount; i++) {
			boolean includeUpper = i == binCount - 1;
			relevancesDataset.addBin(new SimpleHistogramBin(binSize * i, binSize * (i + 1), true, includeUpper));
		}
		relevancesDataset.addObservations(relevancesArray);
		JFreeChart relevancesChart = ChartFactory.createHistogram("Email Thread Relevance", "Relevance", "Frequency", relevancesDataset);
		XYBarRenderer relevancesBarRenderer = (XYBarRenderer) relevancesChart.getXYPlot().getRenderer();
		relevancesBarRenderer.setBarPainter(new StandardXYBarPainter());
		ChartUtils.saveChartAsPNG(myDir.resolve("relevances.png").toFile(), relevancesChart, 500, 500);
	}
}
