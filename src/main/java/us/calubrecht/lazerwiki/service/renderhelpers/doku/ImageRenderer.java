package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ImageRenderer  extends TreeRenderer {
    final Logger logger = LogManager.getLogger(getClass());
    @Override
    public List<Class<? extends ParseTree>> getTargets() {
        return List.of(DokuwikiParser.ImageContext.class);
    }

    @Override
    public StringBuffer render(ParseTree tree, RenderContext renderContext) {
        String inner = renderChildren(getChildren(tree, 1, tree.getChildCount()-1), renderContext).toString();
        return parseInner(inner);
    }

    @Override
    public StringBuffer renderToPlainText(ParseTree tree, RenderContext renderContext) {
        String inner = renderChildren(getChildren(tree, 1, tree.getChildCount()-1), renderContext).toString();
        Map<INNARD_TOKEN, String> innards = splitInnards(inner);
        if (innards.get(INNARD_TOKEN.TITLE) != null) {
            return new StringBuffer(innards.get(INNARD_TOKEN.TITLE));
        }
        return new StringBuffer(innards.get(INNARD_TOKEN.FILE_NAME));
    }

    final Pattern innardsPattern = Pattern.compile("^(?<fileTok> *(?<fileName>[\\w.:\\-]+)(\\?(?<options>\\w+(&\\w+)*))? *)(\\|(?<title>.*))?$");

    enum INNARD_TOKEN {FILE_NAME, FILE_TOK, OPTIONS, TITLE}

    Map<INNARD_TOKEN, String> splitInnards(String inner) {
        Map<INNARD_TOKEN, String> ret = new HashMap<>();
        Matcher match = innardsPattern.matcher(inner);
        if (!match.matches()) {
            logger.error("Cannot parse image markup: " + inner);
            ret.put(INNARD_TOKEN.FILE_NAME, inner.split("\\?")[0]);
            ret.put(INNARD_TOKEN.FILE_TOK, inner.split("\\?")[0]);
            return ret;
        }
        ret.put(INNARD_TOKEN.FILE_TOK, match.group("fileTok"));
        ret.put(INNARD_TOKEN.FILE_NAME, match.group("fileName"));
        ret.put(INNARD_TOKEN.OPTIONS, match.group("options"));
        ret.put(INNARD_TOKEN.TITLE, match.group("title"));
        return ret;

    }

    protected StringBuffer renderChildren(List<ParseTree> trees, RenderContext renderContext) {
        StringBuffer outBuffer = new StringBuffer();
        List<ParseTree> childrenToMerge = new ArrayList<>();
        String lastChildClass = null;
        for(ParseTree child: trees) {
            outBuffer.append(child.getText());
        }
        return outBuffer;
    }

    static final Pattern SIZE_PATTERN= Pattern.compile("[0-9]+(x[0-9]+)?");
    String getSizeTok(String options) {
        if (options == null) {
            return "";
        }
        String[] toks = options.split("&");
        for (String tok : toks) {
            if (SIZE_PATTERN.matcher(tok).matches()) {
                return "?" + tok;
            }
        }
        return "";
    }

    StringBuffer parseInner(String inner) {
        StringBuffer sb = new StringBuffer();
        Map<INNARD_TOKEN, String> innards = splitInnards(inner);
        String className = "media";
        String imageTok = innards.get(INNARD_TOKEN.FILE_TOK);
        if (imageTok.startsWith(" ") && imageTok.endsWith(" ")) {
            className = "mediacenter";
        }
        else if (imageTok.startsWith(" ")) {
           className = "mediaright";
        }
        else if (imageTok.endsWith(" ")) {
            className = "medialeft";
        }
        sb.append("<img src=\"/_media/");
        sb.append(innards.get(INNARD_TOKEN.FILE_NAME).trim() + getSizeTok(innards.get(INNARD_TOKEN.OPTIONS)));
        String titleText = Strings.isBlank(innards.get(INNARD_TOKEN.TITLE)) ? "" : " title=\"" + innards.get(INNARD_TOKEN.TITLE).trim() + "\"";
        sb.append("\" class=\"" + className + "\"" + titleText + " loading=\"lazy\">");
        return sb;
    }
}