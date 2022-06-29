package nl.andrewl.jvisualizer.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import nl.andrewl.jvisualizer.ChartRenderer;
import nl.andrewl.jvisualizer.JVisualizer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class SearchPerformanceRenderer implements ChartRenderer {
	@Override
	public void renderCharts(JsonObject data) throws Exception {
		JsonObject componentsAndConnectors = data.getAsJsonObject("search_precision_components_and_connectors");
		JsonObject decisionFactors = data.getAsJsonObject("search_precision_decision_factors");
		JsonObject rationale = data.getAsJsonObject("search_precision_rationale");
		JsonObject reusableSolutions = data.getAsJsonObject("search_precision_reusable_solutions");
		Map<String, JsonObject> datas = Map.of(
				"Components and Connectors", componentsAndConnectors,
				"Decision Factors", decisionFactors,
				"Rationale", rationale,
				"Reusable Solutions", reusableSolutions
		);

		XYSeriesCollection allTagsEmailNdcgCollection = new XYSeriesCollection();
		XYSeriesCollection allTagsThreadNdcgCollection = new XYSeriesCollection();
		XYSeriesCollection allTagsEmailPrecisionCollection = new XYSeriesCollection();
		XYSeriesCollection allTagsThreadPrecisionCollection = new XYSeriesCollection();
		for (var entry : datas.entrySet()) {
			String name = entry.getKey();
			JsonObject allTags = entry.getValue().getAsJsonObject("all_tags");
			allTagsEmailNdcgCollection.addSeries(getJsonArraySeries(name, allTags.getAsJsonArray("email_ndcg")));
			allTagsThreadNdcgCollection.addSeries(getJsonArraySeries(name, allTags.getAsJsonArray("thread_ndcg")));
			allTagsEmailPrecisionCollection.addSeries(getJsonArraySeries(name, allTags.getAsJsonArray("email_precision")));
			allTagsThreadPrecisionCollection.addSeries(getJsonArraySeries(name, allTags.getAsJsonArray("thread_precision")));
		}

		renderChart(
				"NDCG for All Tags, Individual Emails",
				allTagsEmailNdcgCollection,
				"Result Count (N)",
				"NDCG Measure",
				"all_tags_email_ndcg.png"
		);
		renderChart(
				"NDCG for All Tags, Email Threads",
				allTagsThreadNdcgCollection,
				"Result Count (N)",
				"NDCG Measure",
				"all_tags_thread_ndcg.png"
		);
		renderChart(
				"Precision for All Tags, Individual Emails",
				allTagsEmailPrecisionCollection,
				"Result Count (N)",
				"Precision Measure",
				"all_tags_email_precision.png"
		);
		renderChart(
				"Precision for All Tags, Email Threads",
				allTagsThreadPrecisionCollection,
				"Result Count (N)",
				"Precision Measure",
				"all_tags_thread_precision.png"
		);
	}

	private void renderChart(String title, XYSeriesCollection collection, String xAxisLabel, String yAxisLabel, String filename) throws IOException {
		JFreeChart chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, collection);
		JVisualizer.getTheme().apply(chart);
		final RectangleInsets padding = new RectangleInsets(2, 2, 2, 40);
		chart.getLegend().setItemLabelPadding(padding);
		XYItemRenderer renderer = chart.getXYPlot().getRenderer();
		renderer.setDefaultStroke(new BasicStroke(4.0f));
		((AbstractRenderer) renderer).setAutoPopulateSeriesStroke(false);
		ChartUtils.saveChartAsPNG(new File(filename), chart, 1500, 700);
	}

	private XYSeries getJsonArraySeries(String name, JsonArray array) {
		XYSeries series = new XYSeries(name);
		int n = 1;
		for (var item : array) {
			series.add(n++, item.getAsDouble());
		}
		return series;
	}
}
