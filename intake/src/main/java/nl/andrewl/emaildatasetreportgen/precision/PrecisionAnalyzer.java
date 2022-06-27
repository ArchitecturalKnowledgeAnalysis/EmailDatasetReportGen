package nl.andrewl.emaildatasetreportgen.precision;

import nl.andrewl.emaildatasetreportgen.AnalysisUtils;

import java.util.Arrays;

public class PrecisionAnalyzer {
	private final RelevanceSupplier relevanceSupplier;
	private final double[] idealRelevances;

	public PrecisionAnalyzer(RelevanceSupplier relevanceSupplier, double[] idealRelevances) {
		this.relevanceSupplier = relevanceSupplier;
		this.idealRelevances = idealRelevances;
	}

	public PrecisionResult analyzeSearch(String query) throws Exception {
		double[] relevances = relevanceSupplier.getRelevances(query);
		double[] ndcgValues = AnalysisUtils.iterativeNdcg(relevances, idealRelevances);
		double[] precisionValues = new double[relevances.length];
		for (int i = 0; i < relevances.length; i++) {
			precisionValues[i] = Arrays.stream(relevances)
					.limit(i + 1)
					.mapToInt(v -> v > 0 ? 1 : 0)
					.average().orElseThrow();
		}
		return new PrecisionResult(ndcgValues, precisionValues);
	}
}
