package us.calubrecht.lazerwiki.syntax.framework;

import jakarta.annotation.PostConstruct;
import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.syntax.framework.ITreeRenderer;
import us.calubrecht.lazerwiki.syntax.parser.HeaderParser;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ParserRegistrar {
    @Autowired
    Set<ITreeRenderer> renderers;
    Map<Class, ITreeRenderer> renderersForClass;
    @Autowired
    Set<ITreeParser> parsers;
    @Autowired
    Set<IInnerParser> innerParsers;
    Map<Character, List<IInnerParser>> innerParsersForKeychar;

    final ITreeRenderer DEFAULT_RENDERER = new ITreeRenderer.DefaultRenderer();

    @PostConstruct
    public void linkBeans() {
        renderersForClass = new ConcurrentHashMap<>();
        for (ITreeRenderer renderer : renderers) {
            renderer.getTargets().forEach(cl -> renderersForClass.put(cl, renderer));
            renderer.setRegistrar(this);
        }
        for (ITreeParser parser : parsers) {
            parser.setRegistrar(this);
        }
        innerParsersForKeychar = new HashMap<>();
        for (IInnerParser parser : innerParsers) {
            parser.setRegistrar(this);
            for(Character keyChar : parser.keyCharacters()) {
                innerParsersForKeychar.computeIfAbsent(keyChar, (k) -> new ArrayList<>()).add(parser);
            }
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

    public List<IInnerParser> getParsersForKeyCharacter(char c) {
        return innerParsersForKeychar.getOrDefault(c, List.of());
    }

}
