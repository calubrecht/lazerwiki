package us.calubrecht.lazerwiki.syntax.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.ParseContext;
import us.calubrecht.lazerwiki.syntax.nodes.MacroNode;

@Component
public class MacroParser extends AbstractTreeParser {
  final String macroStart = "~~MACRO~~";
  final String macroEnd = "~~/MACRO~~";

  final Pattern macroTokenPattern = Pattern.compile("~~(/)?MACRO~~");

  @Override
  public ITreeNode parse(ParseContext parseContext) {
    ParseContext blockLines = new ParseContext();
    ParseContext subparseContext = new ParseContext(parseContext);

    int start = parseContext.getPosition();
    boolean blockEnded = false;
    int lineCount = 0;
    String optionString = "";
    boolean foundLines = false;
    String firstLine = subparseContext.remainingLine();

    // For each line, (including first), search for Macro start and Macro end tags
    // If macro start, increment macro Count, if macroEnd decrement. If macroCount == 0, then end
    // macro

    /*
    int macroCount = countSubstrings(firstLine, macroStart);
    String lastLine = "";
    if (countSubstrings(firstLine, macroEnd) == macroCount) {
        lastLine = firstLine.substring(0, firstLine.lastIndexOf(macroEnd) + 10);
        lineCount = 0;
        blockEnded = true;
    }
    blockLines.addLine(firstLine);
    subparseContext.advanceLine();*/
    int macroCount = 0;
    String lastLine = null;
    while (!subparseContext.isEmpty()) {
      String nextLine = subparseContext.remainingLine();
      Matcher macroMatcher = macroTokenPattern.matcher(nextLine);
      while (macroMatcher.find()) {
        String f = macroMatcher.group();
        if (f.equals(macroStart)) {
          macroCount++;
        } else {
          // macroEnd
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
      // No hiddenEnd found, abort block
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
    /*CharSequence sequence = parseContext.subsequence();
    int start = parseContext.getPosition();
    Matcher matcher = macroPattern.matcher(sequence);
    if (matcher.find()) {
        String text = matcher.group(1);
        String fullText = matcher.group();
        int length = fullText.length();
        MacroNode node = new MacroNode(text, fullText);
        node.setPosition(Pair.of(start, start + length - 1));
        node.setParseContext(parseContext);
        return Pair.of(length, node);
    }
    return null;*/
  }

  @Override
  public boolean canBeginParse(String line) {
    return line.startsWith(macroStart);
  }

  @Override
  public String parserKey() {
    return "MacroParser";
  }
}
