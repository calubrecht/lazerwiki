package us.calubrecht.lazerwiki.examplePlugins;

import us.calubrecht.lazerwiki.plugin.EditToolbarDef;
import us.calubrecht.lazerwiki.plugin.Plugin;
import us.calubrecht.lazerwiki.plugin.WikiPlugin;

import java.util.List;

@WikiPlugin
public class ClearFloats extends Plugin {
    @Override
    public String getName() {
        return "clearFloats";
    }

    @Override
    public List<EditToolbarDef> getEditToolbarDefinitions() {
        String script = """
            (currentText, selectStart, selectEnd) => {
            return {action:"insert", location:-1, value:"\\n~~~MACRO~~wrap:clear~~/MACRO~~"};}
            """;
        return List.of(new EditToolbarDef("Clear Floats", "clearFloats.png", script));
    }
}
