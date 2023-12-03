package us.calubrecht.lazerwiki.macro;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class Macro {

    public abstract String getName();

    public Optional<String> getCSS() {
        return Optional.empty();
    }

    public abstract String render(MacroContext context, String macroArgs);


    public static interface MacroContext {
        String sanitize(String input);

        Pair<String, Map<String, Object>> renderPage(String pageDescriptor);

        List<String> getPagesByNSAndTag(String ns, String tag);
        List<String> getAllPages();

        List<String> getLinksOnPage(String page);

        Pair<String, Map<String, Object>> renderMarkup(String markup);
    }
}
