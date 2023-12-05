package us.calubrecht.lazerwiki.service;

import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.AnnotatedTypeScanner;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.plugin.EditToolbarDef;
import us.calubrecht.lazerwiki.plugin.WikiPlugin;
import us.calubrecht.lazerwiki.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PluginService {
    Logger logger = LogManager.getLogger(getClass());
    @Value("#{'${lazerwiki.plugin.scan.packages}'.split(',')}")
    private List<String> pluginPackages;

    List<Plugin> plugins = new ArrayList<>();
    List<EditToolbarDef> toolbarDefs = new ArrayList<>();


    public void registerPlugin(Plugin plugin) {
        logger.info("Registering plugin " + plugin.getName() + " as " + plugin.getClass());
        plugins.add(plugin);
        toolbarDefs.addAll(plugin.getEditToolbarDefinitions());
    }

    @PostConstruct
    public void registerPlugins() {
        AnnotatedTypeScanner scanner= new AnnotatedTypeScanner(WikiPlugin.class);
        Set<Class<?>> macroClasses = scanner.findTypes(pluginPackages);
        macroClasses.forEach((cl) -> {
            try {
                Plugin plugin = (Plugin)cl.getDeclaredConstructor().newInstance();
                registerPlugin(plugin);

            } catch (Exception e) {
                logger.error("Failed to instantiate a plugin of type " + cl + ".", e);
            }
        });
    }

    public String getEditToolbarDefs() {
        return "var LAZERWIKI_PLUGINS=[%s];".formatted(toolbarDefs.stream().map(def -> def.toJSDefinition()).collect(Collectors.joining(",\n")));
    }

}
