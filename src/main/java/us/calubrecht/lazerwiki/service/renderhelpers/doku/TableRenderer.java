package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.AdditiveTreeRenderer;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.service.renderhelpers.TreeRenderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TableRenderer extends FlatteningRenderer {
    Set<Class> treesToFlatten = Set.of(DokuwikiParser.LineContext.class, DokuwikiParser.Line_itemContext.class, DokuwikiParser.Inner_textContext.class);

    static final Set<String> DATA_TAGS = Set.of("|", "^");

    @Override
    public StringBuffer render(List<ParseTree> trees, RenderContext renderContext) {
        StringBuffer sb = new StringBuffer();
        sb.append("<table class=\"lazerTable\"><tbody>");
        List<ParseTree> children = trees.stream().flatMap(
                (t) -> flattenChildren(t, true).stream()).collect(Collectors.toList());
        String nextOpenTag = null;
        boolean inRow = false;
        TableData currTableData= null;
        boolean closingData = false;
        List<List<TableData>> rows = new ArrayList();
        List<TableData> currRow = new ArrayList<>();
        rows.add(currRow);
        for (ParseTree tree : children) {
            if (DATA_TAGS.contains(tree.getText())) {
                if (currTableData != null) {
                    if (closingData) {
                        currTableData.colspan++;
                    }
                    closingData = true;
                }
                if (tree.getText().equals("|")) {
                    nextOpenTag = "td";
                } else {
                    nextOpenTag = "th";
                }
            } else if (tree.getText().equals("\n") ) {
                currRow.add(currTableData);
                currRow = new ArrayList<>();
                rows.add(currRow);
                currTableData = null;
                closingData = false;
            } else {
                if (closingData) {
                    currRow.add(currTableData);
                    closingData = false;
                    currTableData = null;
                }
                if (currTableData == null) {
                    currTableData = new TableData(nextOpenTag, renderContext);
                }
                currTableData.addInternal(tree);
            }
        }
        rows.forEach(row -> {
            if (row.size() == 0) {
                return;
            }
            sb.append("<tr>");
            row.forEach(data -> {
                sb.append(data.toString());
            });
            sb.append("</tr>\n");
        });
        sb.append("</tbody></table>");
        return sb;
    }

    @Override
    public List<Class> getTargets() {
        return Collections.emptyList();
    }

    @Override
    Set<Class> getTreesToFlatten() {
        return treesToFlatten;
    }

    @Override
    public StringBuffer renderToPlainText(ParseTree tree, RenderContext renderContext) {
        StringBuffer sb = renderChildrenToPlainText(getChildren(tree), renderContext);
        return sb;
    }

    @Override
    public String getAdditiveClass() {
        return "Table";
    }

    class TableData {
        int colspan = 1;
        String tagType;
        List<ParseTree> internal = new ArrayList<>();
        RenderContext renderContext;



        public TableData(String tagType, RenderContext renderContext) {
            this.tagType = tagType;
            this.renderContext = renderContext;
        }

        void addInternal(ParseTree tree) {
            internal.add(tree);
        }

        public String toString() {
            String colspanAttr = " colspan=\"%s\"".formatted(colspan);
            if (colspan ==1) {
                colspanAttr = "";
            }
            return "<%s%s>%s</%s>".formatted(tagType, colspanAttr, renderChildren(internal, renderContext), tagType);
        }
    }
}