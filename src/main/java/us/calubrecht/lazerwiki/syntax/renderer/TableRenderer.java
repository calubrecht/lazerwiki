package us.calubrecht.lazerwiki.syntax.renderer;

import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.ITreeRenderer;
import us.calubrecht.lazerwiki.syntax.nodes.TableNode;

import java.util.Collection;
import java.util.List;

@Component("customSynTableRenderer")
public class TableRenderer extends ContainerRenderer{
    @Override
    public Collection<Class<? extends ITreeNode>> getTargets() {
        return List.of(TableNode.class);
    }

    @Override
    public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<table class=\"lazerTable\"><tbody>");
        buffer.append(super.renderHtml(node, renderContext));
        buffer.append("</tbody></table>\n");
        return buffer;
    }

    @Override
    public StringBuilder renderPlaintext(ITreeNode node, RenderContext renderContext) {
        return new StringBuilder(super.renderPlaintext(node, renderContext).toString().strip());
    }

    @Component
    public static class TableRowRenderer extends ContainerRenderer {
        @Override
        public Collection<Class<? extends ITreeNode>> getTargets() {
            return List.of(TableNode.TableRowNode.class);
        }

        @Override
        public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
            StringBuilder buffer = new StringBuilder();
            buffer.append("<tr>");
            buffer.append(super.renderHtml(node, renderContext));
            buffer.append("</tr>\n");
            return buffer;
        }

        @Override
        public StringBuilder renderPlaintext(ITreeNode node, RenderContext renderContext) {
            StringBuilder buffer = new StringBuilder();
            TableNode.TableRowNode row = (TableNode.TableRowNode)node;
            ITreeRenderer cellRenderer = parserRegistrar.getRenderer(TableNode.TableCellNode.class);
            char lastToken = '\n';
            for (ITreeNode childNode : row.getChildren()) {
                StringBuilder renderedCell = cellRenderer.renderPlaintext(childNode, renderContext);
                lastToken = renderedCell.charAt(renderedCell.length() -1);
                renderedCell.deleteCharAt(renderedCell.length() -1);
                buffer.append(renderedCell);
            }
            buffer.append(lastToken);
            return buffer.append("\n");
        }
    }

    @Component
    public static class TableCellRenderer extends ContainerRenderer {
        @Override
        public Collection<Class<? extends ITreeNode>> getTargets() {
            return List.of(TableNode.TableCellNode.class);
        }

        String getAlign(TableNode.TableCellNode node) {
            return switch (node.getAlignment()) {
                case CENTER ->  " class=\"tableCenter\"";
                case LEFT -> " class=\"tableLeft\"";
                case RIGHT -> " class=\"tableRight\"";
                default -> ""; //NONE
            };
        }

        @Override
        public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
            TableNode.TableCellNode cellNode = (TableNode.TableCellNode)node;
            String tagName = cellNode.getType() == TableNode.TableCellNode.CELL_TYPE.DATA ? "td" : "th";
            String span = cellNode.getColSpan() > 1 ? String.format(" colspan=\"%s\"", cellNode.getColSpan()) : "";
            String align = getAlign(cellNode);
            span += cellNode.getRowSpan() > 1 ? String.format(" rowspan=\"%s\"", cellNode.getRowSpan()) : "";
            StringBuilder buffer = new StringBuilder();
            buffer.append("<").append(tagName).append(align).append(span).append(">");
            buffer.append(super.renderHtml(node, renderContext));
            buffer.append("</").append(tagName).append(">");
            return buffer;
        }

        @Override
        public StringBuilder renderPlaintext(ITreeNode node, RenderContext renderContext) {
            String source = node.getSourceFromContext();
            StringBuilder buffer = new StringBuilder();
            buffer.append(source.charAt(0)).append(super.renderPlaintext(node, renderContext)).append(source.charAt(source.length()-1));
            return buffer;
        }
    }
}
