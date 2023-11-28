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
        return "<div class=\"%s\">%s</div>\n".formatted(toks[0],innerText);
    }
}
