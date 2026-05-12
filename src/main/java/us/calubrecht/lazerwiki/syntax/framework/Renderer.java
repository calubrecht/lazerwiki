package us.calubrecht.lazerwiki.syntax.framework;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class Renderer {
    final ParserRegistrar parserRegistrar;

    public Renderer(@Autowired ParserRegistrar parserRegistrar) {
        this.parserRegistrar = parserRegistrar;
    }


    public String render(ITreeNode node, Map<String, Object> renderState) {
        return null;
    }
}
