package us.calubrecht.lazerwiki.exampleMacros;

import org.apache.commons.lang3.tuple.Pair;
import us.calubrecht.lazerwiki.macro.CustomMacro;
import us.calubrecht.lazerwiki.macro.Macro;

import java.util.Map;
import java.util.Optional;

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
    public String render(MacroContext context, String macroArgs) {
        String includePath = macroArgs.trim();
        Pair<String, Map<String, Object>> p = context.renderPage(includePath);
        context.setPageDontCache();
        if (p.getRight().get("userCanWrite").toString().equals("true")) {
            return p.getLeft() + "<a href=\"/page/%s#Edit\" className=\"includePageLink\">Edit %s</a>".formatted(includePath,includePath);
        }
        return p.getLeft();
    }
}
