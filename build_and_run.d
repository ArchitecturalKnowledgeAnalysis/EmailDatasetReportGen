#!/usr/bin/env dub
/+ dub.sdl:
    dependency "dsh" version="~>1.6.1"
+/
import dsh;
import std.string;
import std.process;

const CURRENT_DATASET_DIR = "/home/andrew/Programming/ArchitecturalKnowledgeAnalysis/Thesis/datasets/current";

void main() {
    build();
}

void build() {
    runOrQuit("mvn clean package");
    string filename = findFilesByExtension("target", ".jar-with-dependencies.jar", false)[0];
    copy(filename, "./reportgen.jar");
    runTopThreads();
}

void runPrecisionAnalysis() {
    auto queries = getLuceneQueries();
    foreach (name, query; queries) {
        print("Getting top threads for lucene query: %s", name);
        string cmd = format!"java -jar reportgen.jar %s analyze-query -q \"%s\" -n 75"(CURRENT_DATASET_DIR, query);
        auto result = executeShell(cmd);
        writeln(result.output);
    }
}

string[string] getLuceneQueries() {
    return [
        "decision_factors": "actor* availab* budget* business case* client* concern* conform* consisten* constraint* context* cost* coupl* customer* domain* driver* effort* enterprise* environment* experience* factor* force* function* goal* integrity interop* issue* latenc* maintain* manage* market* modifiab* objective* organization* performance* portab* problem* purpose* qualit* reliab* requirement* reus* safe* scal* scenario* secur* stakeholder* testab* throughput* usab* user* variability limit* time cohesion efficien* bandwidth speed* need* compat* complex* condition* criteria* resource* accura* complet* suitab* complianc* operabl* employabl* modular* analyz* readab* chang* encapsulat* transport* transfer* migrat* mova* replac* adapt* resilienc* irresponsib* stab* toleran* responsib* matur* accountab* vulnerab* trustworth* verif* protect* certificat* law* flexib* configur* convent* accessib* useful* learn* understand*",
        "reusable_solutions": "action* adapt* alloc* alternativ* approach* asynch* audit* authentic* authoriz* balanc* ballot* beat bridg* broker* cach* capabilit* certificat* chain* challeng* characteristic* checkpoint* choice* cloud composite concrete concurren* confident* connect* credential* decorat* deliver* detect* dual* echo encapsulat* encrypt* esb event* expos* facade factor* FIFO filter* flyweight* framework* function* handl* heartbeat* intermedia* layer* layoff* lazy load lock* mandator* measure* mechanism* memento middleware minut* monitor* mvc observ* offer* opinion* option* orchestrat* outbound* parallel passwords pattern* peer* period* piggybacking ping pipe* platform* point* pool principle* priorit* processor* profil* protect* protocol* prototyp* provid* proxy publish* recover* redundan* refactor* removal replicat* resist* restart restraint* revok* rollback* routine* runtime sanity* schedul* sensor* separat* session* shadow* singleton soa solution* spare* sparrow* specification* stamp* standard* state stor* strap strateg* subscrib* suppl* support* synch* tactic* task* technique* technolog* tier* timer* timestamp* tool* trail transaction* uml unoccupied* view* visit* vot* wizard* worker*",
        "components_and_connectors": "access* allocat* application* architecture* artifact* attribute* behav* broker* call* cluster* communicat* component* compos* concept* connect* consist* construct* consum* contain* control* coordinat* core criteria* data database* decompos* depend* design* diagram* dynamic element* engine* entit* event* exchang* exist* external filter* function* hardware* independ* information infrastructure input* instance* integr* interac* internal item* job* layer* link* load* logic* machin* memor* messag* model* modul* node* operat* outcom* output* part* peer* platform* port* process* produc* program* project* propert* provid* publish* read* relat* request* resourc* respon* scope separate server* service* shar* source* stor* structur* subscrib* support* system* target* transaction* trigger* runtime realtime network* thread* parallel notif* distribut* backend* frontend* central* persist* queue* concurren* middleware* provid* suppl*",
        "rationale": "advantag* alternativ* appropriate assum* benefit* better best caus* choic* choos* complex* condition* critical decid* decision* eas* evaluat* hard* quick* rational* reason* risk* simpl* strong* tradeoff weak* rational* disadvantag* comparison* pros cons good differen* slow* lightweight overkill* recommend* suggest* propos* outperform* important* versus vs contrast* distinct* fast* heav* boost* drawback* option*",
    ];
}

