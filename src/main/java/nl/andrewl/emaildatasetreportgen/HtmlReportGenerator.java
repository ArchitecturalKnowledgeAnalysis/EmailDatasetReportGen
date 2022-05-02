package nl.andrewl.emaildatasetreportgen;

import nl.andrewl.email_indexer.data.EmailDataset;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.file.Files;
import java.nio.file.Path;

public abstract class HtmlReportGenerator implements ReportGenerator {
	private final TemplateEngine templateEngine;
	private final String templateName;

	public HtmlReportGenerator(TemplateEngine templateEngine, String templateName) {
		this.templateEngine = templateEngine;
		this.templateName = templateName;
	}

	public HtmlReportGenerator(String templateName) {
		this(fromClasspath("/templates/"), templateName);
	}

	private static TemplateEngine fromClasspath(String prefix) {
		TemplateEngine engine = new TemplateEngine();
		ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver(HtmlReportGenerator.class.getClassLoader());
		templateResolver.setPrefix(prefix);
		templateResolver.setSuffix(".html");
		templateResolver.setCacheable(false);
		engine.addTemplateResolver(templateResolver);
		return engine;
	}

	@Override
	public void generate(Path outputPath, EmailDataset ds) throws Exception {
		Context ctx = new Context();
		generate(ctx, ds);
		String result = templateEngine.process(templateName, ctx);
		Files.writeString(outputPath, result);
	}

	protected abstract void generate(Context ctx, EmailDataset ds) throws Exception;
}
