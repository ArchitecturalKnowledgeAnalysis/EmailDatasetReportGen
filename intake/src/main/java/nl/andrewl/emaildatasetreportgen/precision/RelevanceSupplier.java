package nl.andrewl.emaildatasetreportgen.precision;

public interface RelevanceSupplier {
	double[] getRelevances(String query) throws Exception;
}
