package us.calubrecht.lazerwiki.syntax.parser;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.ParseContext;
import us.calubrecht.lazerwiki.syntax.framework.Parser;
import us.calubrecht.lazerwiki.syntax.nodes.BlockQuoteNode;
import us.calubrecht.lazerwiki.syntax.nodes.ContainerNode;
import us.calubrecht.lazerwiki.syntax.nodes.HiddenNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HiddenParser extends AbstractTreeParser {
    final Pattern hiddenPattern = Pattern.compile("^<hidden(( \\w*=\"(.*)\")*)>");
    final String hiddenEnd = "</hidden>";

    @Override
    public ITreeNode parse(ParseContext parseContext) {
        ParseContext blockLines = new ParseContext();
        ParseContext subparseContext = new ParseContext(parseContext);

        int start = parseContext.getPosition();
        boolean blockEnded = false;
        int lineCount = 0;
        String optionString = "";
        boolean foundLines = false;
        while(!subparseContext.isEmpty()) {
            String nextLine = subparseContext.peekLine();
            if (!foundLines) {
                // First line, remove the hiddenTag
                Matcher m = matchStartPattern(nextLine);
                blockLines.setRoot(parseContext, subparseContext.getPosition() + m.group().length());
                optionString = m.group(1);
                nextLine = nextLine.substring(m.group().length());
                foundLines = true;
            }

            lineCount++;
            subparseContext.advanceLine();
            if (nextLine.endsWith(hiddenEnd)) {
                // End of block
                blockEnded = true;
                blockLines.addLine(nextLine.substring(0, nextLine.length() - hiddenEnd.length()));
                break;
            } else {
                blockLines.addLine(nextLine);
            }
        }
        if (!blockEnded) {
            // No hiddenEnd found, abort block
            return null;
        }
        blockLines.lock();
        for (int line =0 ; line< lineCount; line++) {
            parseContext.advanceLine();
        }
        HiddenNode hiddenNode = new HiddenNode();
        hiddenNode.setOptions(parseOptions(optionString));
        hiddenNode.setParseContext(parseContext);
        hiddenNode.setPosition(start, parseContext.getPosition());
        Parser.parse(blockLines, hiddenNode,  registrar.getParsers());
        return hiddenNode;
    }

    final Pattern optionPattern = Pattern.compile("(\\w+)=\"([^\"]*)\"");
    Map<String, String> parseOptions(String optionString) {
        Map<String, String> options = new HashMap<>();
        if (optionString.isEmpty()) {
            return Map.of();
        }

        Matcher matcher = optionPattern.matcher(optionString);

        while (matcher.find()) {
            options.put(matcher.group(1), matcher.group(2));
        }

        return options;
    }

    @Override
    public boolean canBeginParse(String line) {
        return matchStartPattern(line) != null;
    }

    Matcher matchStartPattern(String line) {
        Matcher m = hiddenPattern.matcher(line);
        if (m.find()) {
            return m;
        }
        return null;
    }

    @Override
    public String parserKey() {
        return "Hidden";
    }
}
