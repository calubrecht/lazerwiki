package us.calubrecht.lazerwiki.service;

import jakarta.annotation.PostConstruct;
import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RendererRegistrar {
    @Autowired
    Set<TreeRenderer> renderers;
    Map<Class, TreeRenderer> renderersForClass;
    Map<String, TreeRenderer> renderersForAdditiveClass;

    final TreeRenderer DEFAULT_RENDERER = new TreeRenderer.DefaultRenderer();

    @PostConstruct
    public void linkBeans() {
        renderersForClass = new ConcurrentHashMap<>();
        renderersForAdditiveClass = new ConcurrentHashMap<>();
        for (TreeRenderer renderer : renderers) {
            renderer.getTargets().forEach(cl -> renderersForClass.put(cl, renderer));
            if (renderer.isAdditive()) {
                renderersForAdditiveClass.put(renderer.getAdditiveClass(), renderer);
            }
            renderer.setRenderers(this);
        }
        DEFAULT_RENDERER.setRenderers(this);

    }

    public TreeRenderer getRenderer(Class forClass, ParseTree tree) {
        TreeRenderer renderer = renderersForClass.getOrDefault(forClass, DEFAULT_RENDERER);
        return renderer.getSpecificRenderer(tree);
    }

    public TreeRenderer getRenderer(String additiveClass, ParseTree tree) {
        TreeRenderer renderer = renderersForAdditiveClass.getOrDefault(additiveClass, DEFAULT_RENDERER);
        return renderer.getSpecificRenderer(tree);
    }
}
