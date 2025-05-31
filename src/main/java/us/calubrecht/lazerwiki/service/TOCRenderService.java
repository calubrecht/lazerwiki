package us.calubrecht.lazerwiki.service;

import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.HeaderRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class TOCRenderService {
    public String renderTOC(List<HeaderRef> headers) {
        StringBuilder render = new StringBuilder("<div id=\"lw_TOC\">\n");
        int indents = 1;
        int currentLevel = -1;
        for (int i = 0; i < headers.size(); i++) {
            HeaderRef header = headers.get(i);
            if (currentLevel < header.level()) {
                render.append(fmtIndents(indents)).append("<ol>\n");
                currentLevel = header.level();
                indents++;
            }
            while (currentLevel > header.level()) {
                if (indents > 2) {
                    indents--;
                    render.append(fmtIndents(indents)).append("</ol>\n");
                }
                currentLevel--;
            }
            render.append(fmtIndents(indents)).append(fmtHeader(header)).append("\n");
        }
        indents--;
        for (; indents > 0; indents--) {
            render.append(fmtIndents(indents)).append("</ol>\n");
        }
        render.append("</div>");
        return render.toString();
    }

    public String fmtIndents(int indents) {
        return String.join("", Collections.nCopies(indents, "  "));
    }

    public String fmtHeader(HeaderRef ref) {
        return String.format("<li><a href=\"#%s\">%s</a></li>", ref.id(), ref.header());
    }
}
