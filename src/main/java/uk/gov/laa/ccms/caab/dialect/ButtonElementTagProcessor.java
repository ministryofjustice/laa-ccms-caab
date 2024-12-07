package uk.gov.laa.ccms.caab.dialect;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.HashMap;
import java.util.Map;

public class ButtonElementTagProcessor extends AbstractElementTagProcessor {

    private static final String TAG_NAME = "button";
    private static final int PRECEDENCE = 900;

    public ButtonElementTagProcessor() {
        super(TemplateMode.HTML, "govuk", TAG_NAME, true, null, false, PRECEDENCE);
    }

    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag, IElementTagStructureHandler structureHandler) {

        Map<String, String> attributes = tag.getAttributeMap();
        Map<String, String> params = new HashMap<>();
        IStandardExpressionParser parser = StandardExpressions.getExpressionParser(context.getConfiguration());
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.startsWith("th:")) {
                IStandardExpression expression = parser.parseExpression(context, value);
                String resolvedValue = (String) expression.execute(context);
                params.put(key.replace("th:", ""), resolvedValue);
            } else {
                params.put(key, value);
            }
        }

        String classNames = "govuk-button";
        if (params.containsKey("classes")) {
            classNames += " " + params.get("classes");
        }

        StringBuilder commonAttributes = new StringBuilder();
        commonAttributes.append("class=\"").append(classNames).append("\" data-module=\"govuk-button\"");

        if (params.containsKey("id")) {
            commonAttributes.append(" id=\"").append(params.get("id")).append("\"");
        }

        StringBuilder buttonAttributes = new StringBuilder();
        if (params.containsKey("name")) {
            buttonAttributes.append(" name=\"").append(params.get("name")).append("\"");
        }
        if (params.containsKey("disabled")) {
            buttonAttributes.append(" disabled aria-disabled=\"true\"");
        }
        if (params.containsKey("preventDoubleClick")) {
            buttonAttributes.append(" data-prevent-double-click=\"").append(params.get("preventDoubleClick")).append("\"");
        }

        StringBuilder html = new StringBuilder();
        if (params.containsKey("href")) {
            html.append("<a href=\"").append(params.getOrDefault("href", "#")).append("\" role=\"button\" draggable=\"false\" ").append(commonAttributes).append(">");
            html.append(params.getOrDefault("text", ""));
            html.append("</a>");
        } else {
            html.append("<button type=\"").append(params.getOrDefault("type", "submit")).append("\" ").append(buttonAttributes).append(" ").append(commonAttributes).append(">");
            html.append(params.getOrDefault("text", ""));
            html.append("</button>");
        }

        final IModelFactory modelFactory = context.getModelFactory();
        final IModel model = modelFactory.parse(context.getTemplateData(), html.toString());

        structureHandler.replaceWith(model, false);
    }
}
