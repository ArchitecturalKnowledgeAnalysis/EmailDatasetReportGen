package nl.andrewl.jvisualizer.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import nl.andrewl.jvisualizer.ChartRenderer;
import nl.andrewl.jvisualizer.JVisualizer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.SimpleHistogramBin;
import org.jfree.data.statistics.SimpleHistogramDataset;

import java.io.File;

public class RelevanceRenderer implements ChartRenderer {
	@Override
	public void renderCharts(JsonObject data) throws Exception {
		var relevanceData = data.getAsJsonObject("relevance");
		SimpleHistogramDataset relevancesDataset = new SimpleHistogramDataset("Relevance");
		int binCount = 10;
		double binSize = 1.0 / binCount;
		for (int i = 0; i < binCount; i++) {
			boolean includeUpper = i == binCount - 1;
			relevancesDataset.addBin(new SimpleHistogramBin(binSize * i, binSize * (i + 1), true, includeUpper));
		}
		JsonArray threadRelevancesJsonArray = relevanceData.getAsJsonArray("thread_relevances");
		for (var relevance : threadRelevancesJsonArray) {
			relevancesDataset.addObservation(relevance.getAsDouble());
		}

		JFreeChart chart = ChartFactory.createHistogram("Email Thread Relevance", "Relevance", "Frequency", relevancesDataset);
		JVisualizer.CHART_THEME.apply(chart);
		XYBarRenderer relevancesBarRenderer = (XYBarRenderer) chart.getXYPlot().getRenderer();
		relevancesBarRenderer.setBarPainter(new StandardXYBarPainter());
		chart.getXYPlot().getDomainAxis().setLabel("");

		ChartUtils.saveChartAsPNG(new File("email_thread_relevance_hist.png"), chart, 1000, 750);
	}
}
