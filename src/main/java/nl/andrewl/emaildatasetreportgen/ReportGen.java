package nl.andrewl.emaildatasetreportgen;

import nl.andrewl.email_indexer.data.EmailDataset;

import java.nio.file.Path;

public class ReportGen {
	public static void main(String[] args) throws Exception {
		var ds = EmailDataset.open(Path.of("/home/andrew/Programming/ArchitecturalKnowledgeAnalysis/Thesis/datasets/hadoop-cassandra-tajo")).join();
		new LuceneSearchQueryReportGenerator(new LuceneSearchQueryReportGenerator.Settings(
				"actor* availab* budget* business case* client* concern* conform* consisten* constraint* context* cost* coupl* customer* domain* driver* effort* enterprise* environment* experience* factor* force* function* goal* integrity interop* issue* latenc* maintain* manage* market* modifiab* objective* organization* performance* portab* problem* purpose* qualit* reliab* requirement* reus* safe* scal* scenario* secur* stakeholder* testab* throughput* usab* user* variability limit* time cohesion efficien* bandwidth speed* need* compat* complex* condition* criteria* resource* accura* complet* suitab* complianc* operabl* employabl* modular* analyz* readab* chang* encapsulat* transport* transfer* migrat* mova* replac* adapt* resilienc* irresponsib* stab* toleran* responsib* matur* accountab* vulnerab* trustworth* verif* protect* certificat* law* flexib* configur* convent* accessib* useful* learn* understand*",
				25
		)).generate(Path.of("out.html"), ds);
		ds.close();
	}
}
