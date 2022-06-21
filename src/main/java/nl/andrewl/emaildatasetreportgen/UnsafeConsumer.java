package nl.andrewl.emaildatasetreportgen;

@FunctionalInterface
public interface UnsafeConsumer<T> {
	void accept(T obj) throws Exception;
}
