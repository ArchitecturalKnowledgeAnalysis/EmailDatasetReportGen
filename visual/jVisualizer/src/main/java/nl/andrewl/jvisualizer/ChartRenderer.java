package nl.andrewl.jvisualizer;

import com.google.gson.JsonObject;

public interface ChartRenderer {
	void renderCharts(JsonObject data) throws Exception;
}
