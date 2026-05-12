package us.calubrecht.lazerwiki.syntax.renderer;

import org.commonmark.node.*;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeRenderer;
import us.calubrecht.lazerwiki.syntax.framework.ParserRegistrar;

import java.util.*;

@Component
public class HeaderRenderer  implements ITreeRenderer {
  /*  protected final HtmlNodeRendererContext context;
    private final HtmlWriter html;

    public HeaderRenderer(HtmlNodeRendererContext context) {
        this.context = context;
        this.html = context.getWriter();
    }*/

    @Override
    public Collection<Class> getTargets() {
        return List.of();
    }

    @Override
    public void setRegistrar(ParserRegistrar registrar) {

    }
/*
    @Override
    public Set<Class<? extends Node>> getNodeTypes() {
        return new HashSet<>(Arrays.asList(
                Heading.class));
    }

    @Override
    public void render(Node node) {
        node.accept(this);
    }

    @Override
    public void visit(Heading heading) {
        String htag = "h" + heading.getLevel();
        html.line();
        html.tag(htag, Map.of("id","spaghettti"));
        visitChildren(heading);
        html.tag('/' + htag);
        html.line();
    }*/
}
