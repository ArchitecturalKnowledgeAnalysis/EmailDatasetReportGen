package nl.andrewl.jvisualizer.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import nl.andrewl.jvisualizer.ChartRenderer;
import nl.andrewl.jvisualizer.JVisualizer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CharacteristicRenderer implements ChartRenderer {
	@Override
	public void renderCharts(JsonObject data) throws Exception {
		var characteristicData = data.getAsJsonObject("characteristic");
		generateBoxPlot(
				"Email Body Size",
				characteristicData.getAsJsonObject("body_size"),
				"Body Size (# of characters)",
				"body_size.png"
		);
		generateBoxPlot(
				"Email Word Count",
				characteristicData.getAsJsonObject("word_count"),
				"Word Count (# of words)",
				"word_count.png"
		);
		generateBoxPlot(
				"Thread Size",
				characteristicData.getAsJsonObject("thread_size"),
				"Thread Size (# of emails)",
				"thread_size.png"
		);
		generateBoxPlot(
				"Thread Participation",
				characteristicData.getAsJsonObject("thread_participation"),
				"Thread Participation (# of participants)",
				"thread_participation.png"
		);
	}

	private void generateBoxPlot(String title, JsonObject data, String valueAxisLabel, String filename) throws IOException {
		DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
		dataset.add(mapJsonIntArray(data.getAsJsonArray("any_tag")), "Architectural", "Any Tag");
		dataset.add(
				mapJsonIntArray(data.getAsJsonObject("tag_data").getAsJsonArray("existence")),
				"Architectural",
				"Existence"
		);
		dataset.add(
				mapJsonIntArray(data.getAsJsonObject("tag_data").getAsJsonArray("process")),
				"Architectural",
				"Process"
		);
		dataset.add(
				mapJsonIntArray(data.getAsJsonObject("tag_data").getAsJsonArray("property")),
				"Architectural",
				"property"
		);
		dataset.add(
				mapJsonIntArray(data.getAsJsonObject("tag_data").getAsJsonArray("technology")),
				"Architectural",
				"Technology"
		);
		dataset.add(mapJsonIntArray(data.getAsJsonArray("not_ak")), "Non-architectural", "Any Tag");

		JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(title, "Type", valueAxisLabel, dataset, true);
		JVisualizer.CHART_THEME.apply(chart);
		CategoryPlot plot = chart.getCategoryPlot();
		plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		BoxAndWhiskerRenderer renderer = (BoxAndWhiskerRenderer) plot.getRenderer();
		renderer.setMeanVisible(false);
		renderer.setMaxOutlierVisible(false);
		renderer.setMinOutlierVisible(false);
		renderer.setAutoPopulateSeriesStroke(false);
		renderer.setDefaultStroke(new BasicStroke(8.0f));
		final RectangleInsets padding = new RectangleInsets(2, 2, 2, 40);
		chart.getLegend().setItemLabelPadding(padding);
		ChartUtils.saveChartAsPNG(new File(filename), chart, 1500, 1000);
	}

	private List<Integer> mapJsonIntArray(JsonArray array) {
		List<Integer> values = new ArrayList<>(array.size());
		for (var item : array) {
			values.add(item.getAsInt());
		}
		return values;
	}
}
