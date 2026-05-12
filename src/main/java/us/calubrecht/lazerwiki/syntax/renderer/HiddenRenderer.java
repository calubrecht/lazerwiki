package us.calubrecht.lazerwiki.syntax.renderer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.RandomService;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.nodes.ContainerNode;
import us.calubrecht.lazerwiki.syntax.nodes.HiddenNode;
import us.calubrecht.lazerwiki.syntax.nodes.SpecialSpanNode;
import us.calubrecht.lazerwiki.syntax.nodes.TaggedContainerNode;

import java.util.Collection;
import java.util.List;

@Component("customSynHiddenRenderer")
public class HiddenRenderer extends ContainerRenderer {
    @Autowired
    RandomService randomService;

    @Override
    public Collection<Class<? extends ITreeNode>> getTargets() {
        return List.of(HiddenNode.class);
    }

    String getId() {
        return "hiddenToggle" + randomService.nextInt();
    }

    @Override
    public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
        HiddenNode hidden = (HiddenNode)node;
        StringBuilder outBuffer = new StringBuilder();
        outBuffer.append(String.format("<div class=\"hidden\">"));
        String id = getId();
        String rawTitle = hidden.getOptions().getOrDefault("name","Hidden");
        String hiddenTitle = sanitize(rawTitle);
        if (!rawTitle.equals(hiddenTitle)) {
            String error = String.format("Suspicious hidden tag name at %s. Raw text =[%s]", hidden.getPosition().getLeft(), rawTitle);
            addError(renderContext, error);
        }
        String dataNamed = hidden.getOptions().containsKey("name") ? " data-named=\"true\"" : "";
        validateOptions(hidden, renderContext);
        outBuffer.append("<input id=\"").append(id).append("\" class=\"toggle\" type=\"checkbox\">");
        outBuffer.append("<label for=\"").append(id).append("\" class=\"hdn-toggle\"").append(dataNamed).append(">").append(hiddenTitle).append("</label>");
        outBuffer.append("<div class=\"collapsible\">");
        ContainerNode cn = collapseSingleParagraph(hidden);
        outBuffer.append(super.renderHtml(cn, renderContext).toString().strip());
        outBuffer.append("</div></div>");
        return outBuffer;
    }

    @Override
    public StringBuilder renderPlaintext(ITreeNode node, RenderContext renderContext) {
        HiddenNode hidden = (HiddenNode)node;
        ContainerNode container = (ContainerNode)node;
        StringBuilder buffer = new StringBuilder();
        if (hidden.getOptions().containsKey("name")) {
            buffer.append(hidden.getOptions().get("name")).append(":");
        }
        for(ITreeNode child : container.getChildren()) {
            buffer.append(parserRegistrar.getRenderer(child.getClass()).renderPlaintext(child, renderContext));
            buffer.append("\n\n");
        }
        return new StringBuilder(buffer.toString());
    }

    /**
     * If hidden contain just single paragraph, extract its contents and render those as direct children of hidden
     */
    public ContainerNode collapseSingleParagraph(HiddenNode node) {
        if (node.getChildren().size() == 1) {
            if (node.getChildren().get(0) instanceof TaggedContainerNode tcn && tcn.getType() == TaggedContainerNode.TYPE.PARAGRAPH) {
                ContainerNode cn = new ContainerNode();
                cn.getChildren().addAll(tcn.getChildren());
                cn.setPosition(node.getPosition());
                cn.setParseContext(node.getParseContext());
                return cn;
            }
        }
        return node;
    }

    void validateOptions(HiddenNode hidden, RenderContext renderContext) {
        for (String optionName : hidden.getOptions().keySet()) {
            if (!optionName.equals("name")) {
                addError(renderContext, String.format("Unknown attribute \"%s\" in hidden tag at %s", optionName, hidden.getPosition().getLeft()));
            }
        }
    }
}
