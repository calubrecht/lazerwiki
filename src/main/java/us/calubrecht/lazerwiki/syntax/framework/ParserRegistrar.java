package us.calubrecht.lazerwiki.syntax.framework;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ParserRegistrar {
    @Autowired
    Set<ITreeRenderer> renderers;
    Map<Class<? extends ITreeNode>, ITreeRenderer> renderersForClass;
    @Autowired
    Set<ITreeParser> parsers;
    @Autowired
    Set<IInnerParser> innerParsers;
    Map<Character, List<IInnerParser>> innerParsersForKeychar;
    final ITreeRenderer DEFAULT_RENDERER = new ITreeRenderer.DefaultRenderer();

    List<ITreeParser> sortedParsers;

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
        sortedParsers = parsers.stream().sorted(
                (o1, o2) -> new CompareToBuilder()
                        .append(o1.priority(), o2.priority())
                        .append(o1.parserKey(), o2.parserKey())
                        .build()).toList();
        innerParsersForKeychar = new HashMap<>();
        for (IInnerParser parser : innerParsers) {
            parser.setRegistrar(this);
            for(Character keyChar : parser.keyCharacters()) {
                innerParsersForKeychar.computeIfAbsent(keyChar, (k) -> new ArrayList<>()).add(parser);
            }
        }
        DEFAULT_RENDERER.setRegistrar(this);

    }

    public ITreeRenderer getRenderer(Class<? extends ITreeNode> forClass) {
        return renderersForClass.getOrDefault(forClass, DEFAULT_RENDERER);
    }

    public Collection<ITreeParser> getParsers() {
        return sortedParsers;
    }

    public List<IInnerParser> getParsersForKeyCharacter(char c) {
        return innerParsersForKeychar.getOrDefault(c, List.of());
    }

}
