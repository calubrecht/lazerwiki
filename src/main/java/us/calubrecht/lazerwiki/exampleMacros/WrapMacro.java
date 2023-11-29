package us.calubrecht.lazerwiki.exampleMacros;

import us.calubrecht.lazerwiki.macro.CustomMacro;
import us.calubrecht.lazerwiki.macro.Macro;

@CustomMacro
public class WrapMacro extends Macro {

    @Override
    public String getName() {
        return "wrap";
    }

    @Override
    public String render(Macro.MacroContext context, String macroArgs) {
        String[] toks = macroArgs.split(":");
        String innerText = context.renderMarkup(toks[1]).getLeft();
        // Strip the div that comes from the renderer.
        innerText = innerText.
                substring(5, innerText.length() -6);
        return "<div class=\"%s\">%s</div>".formatted(toks[0],innerText);
    }
}
