package us.calubrecht.lazerwiki.exampleMacros;

import us.calubrecht.lazerwiki.macro.CustomMacro;
import us.calubrecht.lazerwiki.macro.Macro;

import java.util.Optional;

@CustomMacro
public class IncludeMacro extends Macro {

    @Override
    public String getName() {
        return "include";
    }

    @Override
    public String render(MacroContext context, String macroArgs) {
        String text = context.renderPage(macroArgs.trim()).getLeft();
        return text != null ? text : "";
    }
}
