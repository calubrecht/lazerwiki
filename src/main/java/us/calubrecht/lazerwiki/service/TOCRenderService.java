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

        /*       String source = "====== Header 1 ======\n ==== Header 2 ====\n====== Header 3 ======\n===== Header 2 =====\n";
        String headerRender = """
                <div id="lw_TOC">
                  <ol>
                    <li>Header 1</li>
                    <ol>
                      <li>Header 2</li>
                    </ol>
                    <li>Header 3</li>
                    <ol>
                      <li>Header 2</li>
                    </ol>
                  </ol>
                </div>
                """;*/
    }

    public String fmtIndents(int indents) {
        return String.join("", Collections.nCopies(indents, "  "));
    }

    public String fmtHeader(HeaderRef ref) {
        return String.format("<li>%s</li>", ref.header());
    }

    public static record HeaderNode(HeaderRef header, List<HeaderRef> children){

        public HeaderNode(HeaderRef header) {
            this(header, new ArrayList<>());
        }
    }
}
