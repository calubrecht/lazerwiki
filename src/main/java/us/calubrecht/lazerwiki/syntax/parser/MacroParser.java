package us.calubrecht.lazerwiki.syntax.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.ParseContext;
import us.calubrecht.lazerwiki.syntax.nodes.MacroNode;

@Component
public class MacroParser extends AbstractTreeParser {
  static final String MACRO_START = "~~MACRO~~";
  static final String MACRO_END = "~~/MACRO~~";

  final Pattern macroTokenPattern = Pattern.compile("~~(/)?MACRO~~");

  @Override
  public ITreeNode parse(ParseContext parseContext) {
    ParseContext blockLines = new ParseContext();
    ParseContext subparseContext = new ParseContext(parseContext);

    int start = parseContext.getPosition();
    boolean blockEnded = false;
    int lineCount = 0;

    // For each line, (including first), search for Macro start and Macro end tags
    // If macro start, increment macro Count, if MACRO_END decrement. If macroCount == 0, then end
    // macro

    int macroCount = 0;
    String lastLine = null;
    while (!subparseContext.isEmpty()) {
      String nextLine = subparseContext.remainingLine();
      Matcher macroMatcher = macroTokenPattern.matcher(nextLine);
      while (macroMatcher.find()) {
        String f = macroMatcher.group();
        if (f.equals(MACRO_START)) {
          macroCount++;
        } else {
          // MACRO_END
          macroCount--;
          if (macroCount == 0) {
            // Done
            int idx = macroMatcher.end();
            lastLine = nextLine.substring(0, idx);
            blockLines.addLine(lastLine);
            blockEnded = true;
            break;
          }
        }
      }
      if (blockEnded) {
        break;
      }
      lineCount++;
      subparseContext.advanceLine();
      blockLines.addLine(nextLine);
    }
    if (!blockEnded) {
      // No MACRO_END found, abort block
      return null;
    }
    blockLines.lock();
    for (int line = 0; line < lineCount; line++) {
      parseContext.advanceLine();
    }
    parseContext.advanceChars(lastLine.length());
    String t = blockLines.getFullText();
    MacroNode macroNode = new MacroNode(t.substring(9, t.length() - 10), t);
    macroNode.setParseContext(parseContext);
    macroNode.setPosition(start, parseContext.getPosition());
    return macroNode;
  }

  @Override
  public boolean canBeginParse(String line) {
    return line.startsWith(MACRO_START);
  }

  @Override
  public String parserKey() {
    return "MacroParser";
  }
}
