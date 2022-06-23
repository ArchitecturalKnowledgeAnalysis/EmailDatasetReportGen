package nl.andrewl.emaildatasetreportgen.precision;

import org.jfree.data.xy.XYSeries;

public record PrecisionResult(
		double[] ndcg,
		double[] precision
) {
	public XYSeries ndcgSeries() {
		XYSeries series = new XYSeries("NDCG");
		int n = 1;
		for (double v : ndcg) {
			series.add(n++, v);
		}
		return series;
	}

	public XYSeries precisionSeries() {
		XYSeries series = new XYSeries("Precision");
		int n = 1;
		for (double v : precision) {
			series.add(n++, v);
		}
		return series;
	}

	public int length() {
		return Math.min(ndcg.length, precision.length);
	}
}
