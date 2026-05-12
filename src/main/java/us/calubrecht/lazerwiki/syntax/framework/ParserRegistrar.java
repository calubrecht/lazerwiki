package us.calubrecht.lazerwiki.syntax.framework;

import jakarta.annotation.PostConstruct;
import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.syntax.framework.ITreeRenderer;
import us.calubrecht.lazerwiki.syntax.parser.HeaderParser;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ParserRegistrar {
    @Autowired
    Set<ITreeRenderer> renderers;
    Map<Class, ITreeRenderer> renderersForClass;
    @Autowired
    Set<ITreeParser> parsers;

    //Map<String, ITreeRenderer> renderersForAdditiveClass;

    final ITreeRenderer DEFAULT_RENDERER = new ITreeRenderer.DefaultRenderer();

    @PostConstruct
    public void linkBeans() {
        renderersForClass = new ConcurrentHashMap<>();
        //renderersForAdditiveClass = new ConcurrentHashMap<>();
        for (ITreeRenderer renderer : renderers) {
            renderer.getTargets().forEach(cl -> renderersForClass.put(cl, renderer));
            renderer.setRegistrar(this);
        }
        DEFAULT_RENDERER.setRegistrar(this);

    }

    public ITreeRenderer getRenderer(Class forClass) {
        ITreeRenderer renderer = renderersForClass.getOrDefault(forClass, DEFAULT_RENDERER);
        return renderer;
    }

    public Collection<ITreeParser> getParsers() {
        // Need to order?
        return parsers;
    }

}
