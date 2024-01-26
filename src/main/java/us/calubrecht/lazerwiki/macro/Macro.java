package us.calubrecht.lazerwiki.macro;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public abstract class Macro {

    public abstract String getName();

    public Optional<String> getCSS() {
        return Optional.empty();
    }

    public abstract String render(MacroContext context, String macroArgs);

    /**
     * If macroArgs is of the format "key1=val1&key2=val2", will split the argument into a Map<String, String>
     * If macroArgs is in some other format, macro is responsible for parsing it itself.
     */
    public Map<String, String> toArgsMap(String macroArgs) {
        if (macroArgs.isBlank()) {
            return Collections.emptyMap();
        }
        Map<String, String> argsMap = new HashMap<>();
        String[] argVals = macroArgs.split("&");
        for (String argVal : argVals) {
            String[] keyVal = argVal.split("=");
            argsMap.put(keyVal[0], keyVal[1]);
        }
        return argsMap;
    }


    public interface MacroContext {
        String sanitize(String input);

        RenderOutput renderPage(String pageDescriptor);
        RenderOutput getCachedRender(String pageDescriptor);
        Map<String, RenderOutput> getCachedRenders(List<String> pageDescriptors);

        List<String> getPagesByNSAndTag(String ns, String tag);
        List<String> getAllPages();

        List<String> getLinksOnPage(String page);

        RenderOutput renderMarkup(String markup);

        void setPageDontCache();

        boolean isPlaintextRender();

        public static abstract class RenderOutput {
            public abstract String getHtml();
            public abstract Map<String,Object> getState();
        }
    }
}
