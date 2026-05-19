package us.calubrecht.examples.macros;

import us.calubrecht.lazerwiki.macro.Macro;

import java.util.Optional;

/**
 * Example macro that displays a warning message box.
 * 
 * Usage: ~~MACRO~~warning:This is an important warning!~~/MACRO~~
 */
public class WarningMacro extends Macro {

    @Override
    public String getName() {
        return "warning";
    }

    @Override
    public String render(MacroContext context, String macroArgs) {
        String text = macroArgs.trim();
        if (text.isEmpty()) {
            return "<div class='warning-box'><strong>Warning:</strong> (empty message)</div>";
        }

        String sanitizedText = context.sanitize(text);
        return "<div class='warning-box'>" +
               "<strong>⚠ Warning:</strong> " +
               sanitizedText +
               "</div>";
    }

    @Override
    public Optional<String> getCSS() {
        return Optional.of(
            ".warning-box {\n" +
            "  background-color: #fff3cd;\n" +
            "  border: 1px solid #ffeaa7;\n" +
            "  border-left: 4px solid #ffc107;\n" +
            "  padding: 12px 15px;\n" +
            "  margin: 10px 0;\n" +
            "  border-radius: 4px;\n" +
            "  color: #856404;\n" +
            "  font-weight: 500;\n" +
            "}\n" +
            ".warning-box strong {\n" +
            "  color: #d39e00;\n" +
            "}\n"
        );
    }

    @Override
    public boolean allowCache(MacroContext context, String macroArgs) {
        return true;  // Warning messages are static
    }
}
