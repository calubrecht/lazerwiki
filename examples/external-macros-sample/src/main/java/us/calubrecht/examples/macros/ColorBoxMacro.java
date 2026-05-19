package us.calubrecht.examples.macros;

import us.calubrecht.lazerwiki.macro.Macro;

import java.util.Optional;

/**
 * Example macro that wraps text in a colored box.
 * 
 * Usage: ~~MACRO~~colorbox:red:This is red text~~/MACRO~~
 * Supported colors: red, blue, green, yellow, orange
 */
public class ColorBoxMacro extends Macro {

    @Override
    public String getName() {
        return "colorbox";
    }

    @Override
    public String render(MacroContext context, String macroArgs) {
        String[] parts = macroArgs.split(":", 2);
        if (parts.length < 2) {
            return "<div class='colorbox-error'>colorbox requires format: colorbox:color:text</div>";
        }

        String color = parts[0].toLowerCase().trim();
        String text = parts[1];

        // Validate color
        if (!isValidColor(color)) {
            return "<div class='colorbox-error'>Invalid color: " + context.sanitize(color) + 
                   ". Valid colors: red, blue, green, yellow, orange</div>";
        }

        String sanitizedText = context.sanitize(text);
        return "<div class='colorbox colorbox-" + color + "'>" + sanitizedText + "</div>";
    }

    @Override
    public Optional<String> getCSS() {
        return Optional.of(
            ".colorbox {\n" +
            "  padding: 10px 15px;\n" +
            "  margin: 10px 0;\n" +
            "  border-radius: 4px;\n" +
            "  border-left: 4px solid;\n" +
            "  font-weight: 500;\n" +
            "}\n" +
            ".colorbox-red {\n" +
            "  background-color: #ffe6e6;\n" +
            "  border-color: #cc0000;\n" +
            "  color: #660000;\n" +
            "}\n" +
            ".colorbox-blue {\n" +
            "  background-color: #e6f2ff;\n" +
            "  border-color: #0066cc;\n" +
            "  color: #003366;\n" +
            "}\n" +
            ".colorbox-green {\n" +
            "  background-color: #e6ffe6;\n" +
            "  border-color: #00cc00;\n" +
            "  color: #006600;\n" +
            "}\n" +
            ".colorbox-yellow {\n" +
            "  background-color: #ffffcc;\n" +
            "  border-color: #cccc00;\n" +
            "  color: #666600;\n" +
            "}\n" +
            ".colorbox-orange {\n" +
            "  background-color: #ffe6cc;\n" +
            "  border-color: #ff9900;\n" +
            "  color: #664400;\n" +
            "}\n" +
            ".colorbox-error {\n" +
            "  background-color: #fff0f0;\n" +
            "  border-color: #ff0000;\n" +
            "  color: #cc0000;\n" +
            "  padding: 10px 15px;\n" +
            "  border-radius: 4px;\n" +
            "}\n"
        );
    }

    private boolean isValidColor(String color) {
        return color.matches("red|blue|green|yellow|orange");
    }

    @Override
    public boolean allowCache(MacroContext context, String macroArgs) {
        return true;  // This macro output is always the same for the same input
    }
}
