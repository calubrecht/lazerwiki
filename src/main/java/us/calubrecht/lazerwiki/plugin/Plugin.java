package us.calubrecht.lazerwiki.plugin;

import java.util.List;

public abstract class Plugin {

    public abstract String getName();

    public abstract List<EditToolbarDef> getEditToolbarDefinitions();
}
