package nl.andrewl.jvisualizer.render;

import com.google.gson.JsonObject;
import nl.andrewl.jvisualizer.ChartRenderer;
import nl.andrewl.jvisualizer.JVisualizer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

public class CountRenderer implements ChartRenderer {
	public static final List<String> AK_TAGS = List.of("existence", "existence-behavioral", "existence-structural", "process", "property", "technology", "not-ak");

	@Override
	public void renderCharts(JsonObject data) throws Exception {
		JsonObject countData = data.getAsJsonObject("count");
		DefaultCategoryDataset emailCountDataset = new DefaultCategoryDataset();
		for (var item : countData.getAsJsonObject("email_tag_counts").entrySet()) {
			if (!AK_TAGS.contains(item.getKey())) continue;
			emailCountDataset.addValue(item.getValue().getAsInt(), item.getKey(), "test");
		}
		DefaultCategoryDataset threadCountDataset = new DefaultCategoryDataset();
		for (var item : countData.getAsJsonObject("thread_tag_counts").entrySet()) {
			if (!AK_TAGS.contains(item.getKey())) continue;
			threadCountDataset.addValue(item.getValue().getAsInt(), item.getKey(), "test");
		}

		final RectangleInsets padding = new RectangleInsets(2, 2, 2, 40);

		JFreeChart chart = ChartFactory.createBarChart("Individual Email Tag Counts", "Tag", "Count", emailCountDataset);
		JVisualizer.CHART_THEME.apply(chart);
		chart.getLegend().setItemLabelPadding(padding);
		CategoryPlot plot = chart.getCategoryPlot();
		plot.getDomainAxis(0).setVisible(false);
		BarRenderer renderer = (BarRenderer) plot.getRenderer();
		renderer.setBarPainter(new StandardBarPainter());
		renderer.setDrawBarOutline(true);
		CategoryAxis domainAxis = plot.getDomainAxis();
		domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		ChartUtils.saveChartAsPNG(new File("email_tag_counts.png"), chart, 1000, 700);

		JFreeChart chart2 = ChartFactory.createBarChart("Thread Tag Counts", "Tag", "Count", threadCountDataset);
		JVisualizer.CHART_THEME.apply(chart2);
		chart2.getLegend().setItemLabelPadding(padding);
		CategoryPlot plot2 = chart2.getCategoryPlot();
		plot2.getDomainAxis(0).setVisible(false);
		BarRenderer renderer2 = (BarRenderer) plot2.getRenderer();
		renderer2.setBarPainter(new StandardBarPainter());
		renderer2.setDrawBarOutline(true);
		CategoryAxis domainAxis2 = plot2.getDomainAxis();
		domainAxis2.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		ChartUtils.saveChartAsPNG(new File("thread_tag_counts.png"), chart2, 1000, 700);

		List<String> onlyAkTags = List.of("existence", "process", "property", "technology");
		DefaultPieDataset<String> pieDataset = new DefaultPieDataset<>();
		for (var item : countData.getAsJsonObject("email_tag_counts").entrySet()) {
			if (!onlyAkTags.contains(item.getKey())) continue;
			pieDataset.setValue(item.getKey(), item.getValue().getAsInt());
		}
		JFreeChart pieChart = ChartFactory.createPieChart("Decision Types", pieDataset, false, false, false);
		PiePlot<String> piePlot = (PiePlot<String>) pieChart.getPlot();
		for (var tag : onlyAkTags) {
			piePlot.setExplodePercent(tag, 0.10);
		}

		PieSectionLabelGenerator gen = new StandardPieSectionLabelGenerator(
				"{0}: {1}, {2}",
				new DecimalFormat("0"),
				new DecimalFormat("0%")
		);
		piePlot.setLabelGenerator(gen);
		pieChart.setBorderVisible(false);
		JVisualizer.CHART_THEME.apply(pieChart);
		ChartUtils.saveChartAsPNG(new File("decision_types_pie.png"), pieChart, 1200, 1200);
	}
}
