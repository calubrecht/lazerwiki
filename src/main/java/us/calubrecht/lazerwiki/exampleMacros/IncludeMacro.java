package us.calubrecht.lazerwiki.exampleMacros;

import org.apache.commons.lang3.tuple.Pair;
import us.calubrecht.lazerwiki.macro.CustomMacro;
import us.calubrecht.lazerwiki.macro.Macro;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.List;

import static us.calubrecht.lazerwiki.model.RenderResult.RENDER_STATE_KEYS.LINKS;

@CustomMacro
public class IncludeMacro extends Macro {
    static final String CSS = """
            a.includePageLink {
              border-color: black;
              border-style: outset;
              background: silver;
              padding: .1em .25em;
              float: right;
              color:black;
              text-decoration:none;
            }
            a.includePageLink:visited {
              color:black;
            }
            a.includePageLink:active {
              color:black;
              border-style: inset;
            }
            """;

    @Override
    public String getName() {
        return "include";
    }

    @Override
    public Optional<String> getCSS() {
        return Optional.of(CSS);
    }

    @Override
    public boolean allowCache(MacroContext context, String macroArgs) {
        String includePath = macroArgs.trim();
        context.addLinks(List.of(includePath));
        return false;}

    @Override
    public String render(MacroContext context, String macroArgs) {
        if (context.isPlaintextRender()) {
            return "";
        }
        String includePath = macroArgs.trim();
        context.addLinks(List.of(includePath));
        MacroContext.RenderOutput p = context.renderPage(includePath);
        context.setPageDontCache();
        if (p.getState().get("userCanWrite").toString().equals("true")) {
            return "<div class=\"include\">%s<a href=\"/page/%s#Edit\" className=\"includePageLink\">Edit %s</a></div>".formatted(p.getHtml(), includePath,includePath);
        }
        return "<div class=\"include\">%s</div>".formatted(p.getHtml());
    }
}
