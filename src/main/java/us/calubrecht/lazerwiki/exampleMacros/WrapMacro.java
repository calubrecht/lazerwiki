package us.calubrecht.lazerwiki.exampleMacros;

import us.calubrecht.lazerwiki.macro.CustomMacro;
import us.calubrecht.lazerwiki.macro.Macro;

import java.util.Collection;

import static us.calubrecht.lazerwiki.model.RenderResult.RENDER_STATE_KEYS.LINKS;

@CustomMacro
public class WrapMacro extends Macro {

    @Override
    public String getName() {
        return "wrap";
    }

    @Override
    public String render(Macro.MacroContext context, String macroArgs) {
        String[] toks = macroArgs.split(":",2);
        if (toks.length == 1 || toks[1].isBlank()) {
            return "<div class=\"%s\"></div>".formatted(toks[0]);
        }
        boolean multiLine = toks[1].indexOf("\n") != -1;
        MacroContext.RenderOutput renderOutput = context.renderMarkup(toks[1]);
        String innerText = renderOutput.getHtml();
        if (!multiLine) {
            // Strip the div that comes from the renderer.
            innerText = innerText.
                    substring(5, innerText.length() - 6);
        }
        context.addLinks((Collection<String>)(renderOutput.getState().get(LINKS.name())));
        return "<div class=\"%s\">%s</div>".formatted(toks[0],innerText);
    }
}
