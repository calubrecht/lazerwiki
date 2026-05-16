package us.calubrecht.lazerwiki.syntax.parser;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.ParseContext;
import us.calubrecht.lazerwiki.syntax.nodes.ImageNode;
import us.calubrecht.lazerwiki.syntax.nodes.ImageNode.ALIGN_TYPE;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ImageParser extends AbstractInnerParser{
    final Pattern pattern = Pattern.compile("^\\{\\{(\\s)*(.*?)(\\s)*(\\|(.*?))?}}", Pattern.DOTALL);

    @Override
    public Collection<Character> keyCharacters() {
        return List.of('{');
    }

    @Override
    public Pair<Integer, ITreeNode> parse(ParseContext parseContext) {
        CharSequence sequence = parseContext.subsequence();
        int start = parseContext.getPosition();
        Matcher matcher = pattern.matcher(sequence);
        if (matcher.find()) {
           ImageInfo info = parseSource(matcher.group(2));
           int length = matcher.group(0).length();
           String title = matcher.group(5) != null ? matcher.group(5).strip() : null;
           ImageNode node = new ImageNode(info.src(), title, info.options(), parseAlignment(matcher));
           node.setPosition(Pair.of(start, start + length -1));
           node.setParseContext(parseContext);
           return Pair.of(length, node);
        }
        return null;
    }

    ImageInfo parseSource(String srcFmt) {
        String[] parts = srcFmt.split("\\?");
        String options = parts.length > 1 ? parts[1] : "";
        return new ImageInfo(parts[0].strip(), options);
    }

    ALIGN_TYPE parseAlignment(Matcher matcher) {
        String preSpaces = matcher.group(1);
        String postSpaces = matcher.group(3);
        if (preSpaces != null && postSpaces != null) {
            return ALIGN_TYPE.CENTER;
        }
        if (preSpaces != null) {
            return ALIGN_TYPE.RIGHT;
        }
        if (postSpaces != null) {
            return ALIGN_TYPE.LEFT;
        }
        return ALIGN_TYPE.NONE;
    }

    record ImageInfo(String src, String options) {
    }

}
