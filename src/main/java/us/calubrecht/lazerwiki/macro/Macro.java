package us.calubrecht.lazerwiki.macro;

public abstract class Macro {

    public abstract String getName();

    public abstract String render(MacroContext context, String macroArgs);


    public static interface MacroContext {
        String sanitize(String input);

        String renderPage(String pageDescriptor);

    }
}
