package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;

@Component
public class ImageRenderer  extends TreeRenderer {
    public Class getTarget() {
        return DokuwikiParser.ImageContext.class;
    }

    public StringBuffer render(ParseTree tree) {
        String inner = renderChildren(getChildren(tree, 1, tree.getChildCount()-1)).toString();
        return parseInner(inner);
    }

    StringBuffer parseInner(String inner) {
        StringBuffer sb = new StringBuffer();
        String imageTok = inner;
        String className = "media";
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
        sb.append(imageTok.trim());
        sb.append("\" class=\"" + className + "\" loading=\"lazy\">");
        return sb;
    }
}