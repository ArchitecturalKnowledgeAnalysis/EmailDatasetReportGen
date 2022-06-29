package nl.andrewl.jvisualizer.render;

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
import java.util.List;
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
		for (var entry : datas.entrySet()) {
			String name = entry.getKey();
			XYSeries emailNdcgSeries = new XYSeries(name);
			int n = 1;
			for (var item : entry.getValue().getAsJsonObject("all_tags").getAsJsonArray("email_ndcg")) {
				emailNdcgSeries.add(n++, item.getAsDouble());
			}
			XYSeries threadNdcgSeries = new XYSeries(name);
			n = 1;
			for (var item : entry.getValue().getAsJsonObject("all_tags").getAsJsonArray("thread_ndcg")) {
				threadNdcgSeries.add(n++, item.getAsDouble());
			}
			allTagsEmailNdcgCollection.addSeries(emailNdcgSeries);
			allTagsThreadNdcgCollection.addSeries(threadNdcgSeries);
		}

		final RectangleInsets padding = new RectangleInsets(2, 2, 2, 40);

		JFreeChart allTagsEmailNdcgChart = ChartFactory.createXYLineChart("NDCG For All Tags, Individual Emails", "N", "NDCG", allTagsEmailNdcgCollection);
		JVisualizer.getTheme().apply(allTagsEmailNdcgChart);
		allTagsEmailNdcgChart.getLegend().setItemLabelPadding(padding);
		XYItemRenderer renderer = allTagsEmailNdcgChart.getXYPlot().getRenderer();
		renderer.setDefaultStroke(new BasicStroke(4.0f));
		((AbstractRenderer) renderer).setAutoPopulateSeriesStroke(false);
		ChartUtils.saveChartAsPNG(new File("all_tags_email_ndcg.png"), allTagsEmailNdcgChart, 1500, 700);

		JFreeChart allTagsThreadNdcgChart = ChartFactory.createXYLineChart("NDCG For All Tags, Threads", "N", "NDCG", allTagsThreadNdcgCollection);
		JVisualizer.getTheme().apply(allTagsThreadNdcgChart);
		allTagsThreadNdcgChart.getLegend().setItemLabelPadding(padding);
		XYItemRenderer renderer2 = allTagsThreadNdcgChart.getXYPlot().getRenderer();
		renderer2.setDefaultStroke(new BasicStroke(4.0f));
		((AbstractRenderer) renderer2).setAutoPopulateSeriesStroke(false);
		ChartUtils.saveChartAsPNG(new File("all_tags_thread_ndcg.png"), allTagsThreadNdcgChart, 1500, 700);
	}
}
