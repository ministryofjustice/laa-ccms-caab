package uk.gov.laa.ccms.caab.util;

import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import uk.gov.laa.springboot.dialect.GovUkDialect;
import uk.gov.laa.springboot.dialect.MojCustomDialect;

public final class ThymeleafTestUtils {

  private ThymeleafTestUtils() {}

  public static ThymeleafViewResolver createViewResolver() {
    final GenericWebApplicationContext context =
        new GenericWebApplicationContext(new MockServletContext());
    context.refresh();
    final SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
    resolver.setApplicationContext(context);
    resolver.setPrefix("classpath:/templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");

    final ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
    messages.setBasename("messages");
    messages.setDefaultEncoding("UTF-8");
    final SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.setTemplateEngineMessageSource(messages);
    engine.addDialect(new GovUkDialect());
    engine.addDialect(new MojCustomDialect());

    final ThymeleafViewResolver views = new ThymeleafViewResolver();
    views.setTemplateEngine(engine);
    views.setCharacterEncoding("UTF-8");
    return views;
  }
}
