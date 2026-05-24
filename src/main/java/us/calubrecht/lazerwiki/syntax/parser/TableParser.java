package us.calubrecht.lazerwiki.syntax.parser;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.framework.*;
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
            else {
                // End of Table
                break;
            }
        }
        TableNode node = new TableNode();
        node.setPosition(Pair.of(start, parseContext.getPosition()));
        node.setParseContext(parseContext);
        int lineStart = start;
        List<List<ITreeNode>> cellMatrix = new ArrayList<>();
        for (String line : tableLines) {
            TableNode.TableRowNode row = new TableNode.TableRowNode();
            row.setParseContext(parseContext);
            ParseContext rowContext = new ParseContext(line, lineStart, parseContext.getRootContext());
            parseCells(line, lineStart, rowContext).stream().forEach(row::addChild);

            cellMatrix.add(row.getChildren());
            node.addChild(row);
            row.setPosition(Pair.of(lineStart, lineStart + line.length() -1));
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
            StringBuilder buffer = new StringBuilder();
            List<TableNode.TableCellNode> cells = new ArrayList<>();
            int cellStart = parseContext.getPosition();
            int bufferStart = cellStart + 1;
            char token = parseContext.peekChar();
            TableNode.TableCellNode currentCell = new TableNode.TableCellNode(token == '|' ? TableNode.TableCellNode.CELL_TYPE.DATA : TableNode.TableCellNode.CELL_TYPE.HEADER);
            currentCell.setParseContext(parseContext);
            parseContext.advanceChar(); // Past First token
            while(!parseContext.isEmpty()) {
                char c = parseContext.peekChar();
                if (Character.isAlphabetic(c) || Character.isDigit(c)) {
                    buffer.append(c);
                    parseContext.advanceChar();
                    continue;
                }
                if (c == '^' || c == '|') {
                    // Cell boundary outside of other parsings.
                    token = c;
                    if (!buffer.isEmpty()) {
                        TextNode node = new TextNode(buffer.toString());
                        node.setParseContext(parseContext);
                        node.setPosition(bufferStart, parseContext.getPosition()-1);
                        if (currentCell.getChildren().isEmpty() && startsSpaces(node)) {
                            currentCell.setAlignment(TableNode.TableCellNode.ALIGNMENT.RIGHT);
                        }
                        if (endsSpaces(node)) {
                            currentCell.setAlignment(currentCell.getAlignment() == TableNode.TableCellNode.ALIGNMENT.RIGHT ? TableNode.TableCellNode.ALIGNMENT.CENTER : TableNode.TableCellNode.ALIGNMENT.LEFT);
                        }
                        currentCell.addChild(node);
                    }
                    currentCell.setPosition(cellStart, parseContext.getPosition());
                    if (!currentCell.getChildren().isEmpty()) {
                        if (parseRowspan(currentCell)) {
                            currentCell = new TableNode.TableCellNode(TableNode.TableCellNode.CELL_TYPE.ROWSPAN_MARKER);
                        }
                        cells.add(currentCell);
                    }
                    else {
                          TableNode.TableCellNode lastCell = cells.get(cells.size()-1);
                          lastCell.setColSpan(lastCell.getColSpan() + 1);
                    }
                    currentCell = new TableNode.TableCellNode(token == '|' ? TableNode.TableCellNode.CELL_TYPE.DATA : TableNode.TableCellNode.CELL_TYPE.HEADER);
                    currentCell.setParseContext(parseContext);
                    cellStart = parseContext.getPosition();
                    parseContext.advanceChar();
                    buffer.setLength(0);
                    bufferStart = parseContext.getPosition();
                    continue;
                }
                List<IInnerParser> parsers = registrar.getParsersForKeyCharacter(c);
                boolean parseFound = false;
                for (IInnerParser parser: parsers) {
                    Pair<Integer, ITreeNode> parsed = parser.parse(parseContext);
                    if (parsed != null) {
                        parseFound = true;
                        if (!buffer.isEmpty()) {
                            TextNode node = new TextNode(buffer.toString());
                            node.setParseContext(parseContext);
                            node.setPosition(bufferStart, parseContext.getPosition() - 1);
                            if (startsSpaces(node)) {
                                currentCell.setAlignment(TableNode.TableCellNode.ALIGNMENT.RIGHT);
                            }
                            currentCell.addChild(node);
                        }
                        buffer.setLength(0);
                        bufferStart = parseContext.getPosition();
                        currentCell.addChild(parsed.getRight());
                        parseContext.advanceChars(parsed.getLeft());
                    }
                }
                if (!parseFound) {
                    buffer.append(c);
                    parseContext.advanceChar();
                }
            }
            currentCell.setPosition(cellStart, parseContext.getPosition());
            return cells;
    }

    boolean parseRowspan(TableNode.TableCellNode node) {
        if (node.getChildren().size() == 1) {
            if (node.getChildren().get(0) instanceof TextNode tn) {
                return tn.asString().strip().equals("::");
            }
        }
        return false;
    }

    boolean startsSpaces(TextNode node) {
        return node.asString().startsWith(" ");
    }

    boolean endsSpaces(TextNode node) {
        return node.asString().endsWith(" ");
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