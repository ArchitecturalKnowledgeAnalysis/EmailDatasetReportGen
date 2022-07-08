package nl.andrewl.jvisualizer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import nl.andrewl.jvisualizer.render.CharacteristicRenderer;
import nl.andrewl.jvisualizer.render.CountRenderer;
import nl.andrewl.jvisualizer.render.RelevanceRenderer;
import nl.andrewl.jvisualizer.render.SearchPerformanceRenderer;
import org.jfree.chart.ChartTheme;
import org.jfree.chart.StandardChartTheme;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JVisualizer {
	public static final List<String> AK_TAGS = List.of("existence", "process", "property", "technology");
	public static final ChartTheme CHART_THEME = getTheme();

	public static void main(String[] args) throws Exception {
		Gson gson = new Gson();
		JsonObject data = gson.fromJson(Files.readString(Path.of(args[0])), JsonObject.class);
		List<ChartRenderer> renderers = List.of(
				new CountRenderer(),
				new SearchPerformanceRenderer(),
				new CharacteristicRenderer(),
				new RelevanceRenderer()
		);
		List<Thread> threads = new ArrayList<>(renderers.size());
		for (var renderer : renderers) {
			var t = new Thread(() -> {
				try {
					renderer.renderCharts(data);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			threads.add(t);
			t.start();
		}
		for (var thread : threads) thread.join();
	}

	public static ChartTheme getTheme() {
		String fontName = "Open Sans";
		StandardChartTheme theme = (StandardChartTheme) StandardChartTheme.createJFreeTheme();
		theme.setSmallFont(new Font(fontName, Font.BOLD, 22));
		theme.setRegularFont(new Font(fontName, Font.BOLD, 28));
		theme.setLargeFont(new Font(fontName, Font.BOLD, 34));
		theme.setExtraLargeFont(new Font(fontName, Font.BOLD, 48));
		theme.setChartBackgroundPaint(Color.WHITE);
		theme.setPlotBackgroundPaint(Color.WHITE);

		return theme;
	}
}
