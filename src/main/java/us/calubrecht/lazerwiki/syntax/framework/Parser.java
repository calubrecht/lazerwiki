package us.calubrecht.lazerwiki.syntax.framework;

import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.syntax.nodes.ContainerNode;
import us.calubrecht.lazerwiki.syntax.nodes.TextNode;

@Component
public class Parser {
  final ParserRegistrar parserRegistrar;

  public Parser(@Autowired ParserRegistrar parserRegistrar) {
    this.parserRegistrar = parserRegistrar;
  }

  public ITreeNode parse(String markup) {
    if (markup.isEmpty()) {
      ContainerNode rootNode = new ContainerNode();
      ParseContext parseContext = new ParseContext();
      rootNode.setParseContext(parseContext.getRootContext());
      rootNode.setPosition(0, 0);
      return rootNode;
    }
    ParseContext parseContext = new ParseContext(markup);
    ContainerNode rootNode = new ContainerNode();
    rootNode.setParseContext(parseContext.getRootContext());
    rootNode.setPosition(0, markup.length() - 1);
    return parse(parseContext, rootNode, parserRegistrar.getParsers());
  }

  public static ITreeNode parse(
      ParseContext parseContext, ContainerNode container, Collection<ITreeParser> parsers) {
    while (!parseContext.isEmpty()) {
      ITreeNode nextNode = null;
      for (ITreeParser parser : parsers) {
        if (!parser.canBeginParse(parseContext.remainingLine())) {
          continue;
        }
        nextNode = parser.parse(parseContext);
        if (nextNode != null) {
          break;
        }
      }
      if (nextNode == null) {
        int nodeStart = parseContext.getPosition();
        String nextLine = parseContext.remainingLine();
        nextNode = new TextNode(parseContext.remainingLine());
        parseContext.advanceLine();
        nextNode.setParseContext(parseContext.getRootContext());
        nextNode.setPosition(Pair.of(nodeStart, nodeStart + nextLine.length() - 1));
      }
      container.addChild(nextNode);
    }
    validateNode(container);
    validateContainerContext(container);
    return container;
  }

  static void validateNode(ITreeNode node) {
    if (node.getParseContext() == null || node.getPosition() == null) {
      throw new RuntimeException("Node missing Context or position- " + node.getClass());
    }
  }

  static void validateContainerContext(ContainerNode container) {
    for (ITreeNode node : container.getChildren()) {
      validateNode(node);
    }
    for (ITreeNode node : container.getChildren()) {
      if (node instanceof ContainerNode cn) {
        validateContainerContext(cn);
      }
    }
  }

  public static void parseInner(
      ParseContext parseContext, ContainerNode parentNode, ParserRegistrar parserRegistrar) {
    StringBuilder buffer = new StringBuilder();
    int textStart = parseContext.getPosition();
    while (!parseContext.isEmpty()) {
      char c = parseContext.peekChar();
      if (Character.isAlphabetic(c) || Character.isDigit(c) || c == '\n') {
        buffer.append(c);
        parseContext.advanceChar();
        continue;
      }
      List<IInnerParser> parsers = parserRegistrar.getParsersForKeyCharacter(c);
      boolean parseFound = false;
      for (IInnerParser parser : parsers) {
        Pair<Integer, ITreeNode> parsed = parser.parse(parseContext);
        if (parsed != null) {
          parseFound = true;
          parentNode.addChild(textNode(buffer, textStart, parentNode.getParseContext()));
          buffer = new StringBuilder();
          parentNode.addChild(parsed.getRight());
          textStart = parseContext.getPosition();
          parseContext.advanceChars(parsed.getLeft());
        }
      }
      if (!parseFound) {
        buffer.append(c);
        parseContext.advanceChar();
      }
    }
    parentNode.addChild(textNode(buffer, textStart, parentNode.getParseContext()));
  }

  static TextNode textNode(StringBuilder buffer, int position, ReadOnlyParseContext parseContext) {
    if (buffer.isEmpty()) {
      return null;
    }
    TextNode node = new TextNode(buffer.toString());
    node.setPosition(Pair.of(position, position + node.asString().length() - 1));
    node.setParseContext(parseContext);
    return node;
  }
}
