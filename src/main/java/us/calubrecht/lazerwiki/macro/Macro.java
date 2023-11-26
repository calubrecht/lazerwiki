package us.calubrecht.lazerwiki.macro;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

public abstract class Macro {

    public abstract String getName();

    public abstract String render(MacroContext context, String macroArgs);


    public static interface MacroContext {
        String sanitize(String input);

        Pair<String, Map<String, Object>> renderPage(String pageDescriptor);

        List<String> getPagesByNSAndTag(String ns, String tag);
    }
}
