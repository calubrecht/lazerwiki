package us.calubrecht.lazerwiki.service;

import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    public void linkBeans() {
        renderersForClass = new ConcurrentHashMap<>();
        for (TreeRenderer renderer : renderers) {
            renderersForClass.put(renderer.getTarget(), renderer);
            renderer.setRenderers(this);
        }
        TreeRenderer.DEFAULT.setRenderers(this);

    }

    public TreeRenderer getRenderer(Class forClass) {
        return renderersForClass.getOrDefault(forClass, TreeRenderer.DEFAULT);
    }
}
