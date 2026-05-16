package us.calubrecht.lazerwiki.syntax.parser;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.ParseContext;
import us.calubrecht.lazerwiki.syntax.framework.Parser;
import us.calubrecht.lazerwiki.syntax.nodes.TableNode;
import us.calubrecht.lazerwiki.syntax.nodes.TextNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TableParser extends AbstractTreeParser {
    final Pattern tablePattern = Pattern.compile("[|^].*[|^]");

    @Override
    public ITreeNode parse(ParseContext parseContext) {
        ParseContext tableLines = new ParseContext();
        tableLines.setRoot(parseContext, parseContext.getPosition());
        int start = parseContext.getPosition();
        while(!parseContext.isEmpty()) {
            String nextLine = parseContext.peekLine();
            Matcher m = tablePattern.matcher(nextLine);
            if (m.matches()) {
                tableLines.addLine(nextLine);
                parseContext.advanceLine();
            }
            else if (tableLines.isEmpty() ){
               // No table Found
               return null;
            }
        }
        TableNode node = new TableNode();
        node.setPosition(Pair.of(start, parseContext.getPosition()));
        node.setParseContext(parseContext);
        int lineStart = start;
        List<List<ITreeNode>> cellMatrix = new ArrayList<>();
        for (String line : tableLines) {
            // Create a TableNode.TableRowNode
            // Split row into cells
            // For each, Create a TableNode.TableCell (containerNode)
            // Then run parse inside.
            TableNode.TableRowNode row = new TableNode.TableRowNode();
            parseCells(line, lineStart, parseContext).stream().forEach(row::addChild);
            cellMatrix.add(row.getChildren());
            node.addChild(row);
            row.setPosition(Pair.of(lineStart, line.length() -1));
            row.setParseContext(parseContext);
            lineStart += line.length() + 1;
        }
        // Count and process ROWSPAN_MARKERS
        final TableNode.TableCellNode.CELL_TYPE marker = TableNode.TableCellNode.CELL_TYPE.ROWSPAN_MARKER;
        for (int i = cellMatrix.size() -1; i >= 0; i--)  {
            List<ITreeNode> currRow = cellMatrix.get(i);
            for (int j = 0; j < currRow.size(); j++) {
                TableNode.TableCellNode currCell = (TableNode.TableCellNode)currRow.get(j);
                if (currCell.getType() == marker) {
                    for (int k = i-1; k >= 0; k--) {
                        List<ITreeNode> exRow = cellMatrix.get(k);
                        if (exRow.size() <= j) {
                            // This row is not long enough, keep going
                            continue;
                        }
                        TableNode.TableCellNode exCell = (TableNode.TableCellNode)exRow.get(j);
                        if (exCell.getType() == marker) {
                            continue;
                        }
                        exCell.setRowSpan(exCell.getRowSpan() +1);
                        break;
                    }
                    currRow.remove(j);
                    j++;
                }
            }
        }
        return node;
    }

    List<TableNode.TableCellNode> parseCells(String row, int start, ParseContext parseContext) {
        char token = row.charAt(0);
        List<TableNode.TableCellNode> cells = new ArrayList<>();
        TableNode.TableCellNode lastCell = null;
        int cellStart = 1;
        for (int idx = 1; idx < row.length() ; idx ++) {
            int nextChar = row.charAt(idx);
            if (nextChar == '^' || nextChar == '|') {
                // next cell
                String cell = row.substring(cellStart, idx);
                TableNode.TableCellNode cellNode = new TableNode.TableCellNode(token == '|' ? TableNode.TableCellNode.CELL_TYPE.DATA : TableNode.TableCellNode.CELL_TYPE.HEADER);
                cellNode.setPosition(Pair.of(cellStart, cellStart + cell.length() -1));
                cellNode.setParseContext(parseContext);
                if (cell.isEmpty()) {
                    if (cells.isEmpty()) {
                        cells.add(cellNode);
                    } else {
                        lastCell.setColSpan(lastCell.getColSpan()+1);
                        token = row.charAt(idx);
                        cellStart = idx + 1;
                        lastCell = cellNode;
                        continue;
                    }
                }
                else if (cell.strip().equals("::")) {
                    cellNode = new TableNode.TableCellNode(TableNode.TableCellNode.CELL_TYPE.ROWSPAN_MARKER);
                }
                else {
                    ParseContext cellContext = new ParseContext(cell, start + cellStart, parseContext);
                    Parser.parseInner(cellContext, cellNode, registrar);
                    if (cell.startsWith(" ")) {
                        cellNode.setAlignment(cell.endsWith(" ") ? TableNode.TableCellNode.ALIGNMENT.CENTER : TableNode.TableCellNode.ALIGNMENT.RIGHT);
                    }
                    else if (cell.endsWith(" ")) {
                        cellNode.setAlignment(TableNode.TableCellNode.ALIGNMENT.LEFT);
                    }
                }
                token = row.charAt(idx);
                cellStart = idx + 1;
                cells.add(cellNode);
                lastCell = cellNode;
            }

        }
        return cells;
    }

    @Override
    public boolean canBeginParse(String line) {
        Matcher m = tablePattern.matcher(line);
        return m.matches() ;
    }

    @Override
    public String parserKey() {
        return "Table";
    }
}
