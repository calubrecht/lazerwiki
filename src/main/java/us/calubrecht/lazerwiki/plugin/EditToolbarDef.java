package us.calubrecht.lazerwiki.plugin;

public record EditToolbarDef(String name, String icon, String script) {

    public String toJSDefinition() {
        String icon = icon();
        if (icon == null) {
            icon = "toolbarBtn.png";
        }
        return """
                {name:"%s", icon:"%s", script:%s}
                """.formatted(name(), icon(), script());
    }
}
