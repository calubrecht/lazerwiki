package us.calubrecht.lazerwiki.service;

import org.apache.commons.lang3.tuple.Pair;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.*;
import org.commonmark.parser.IncludeSourceSpans;
import org.commonmark.parser.Parser;
import org.commonmark.parser.SourceLine;
import org.commonmark.parser.block.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.HeaderRef;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.framework.ParseContext;
import us.calubrecht.lazerwiki.syntax.framework.Renderer;
import us.calubrecht.lazerwiki.syntax.nodes.*;

import java.util.*;

import static us.calubrecht.lazerwiki.model.RenderStateKeys.*;
import static us.calubrecht.lazerwiki.model.RenderStateKeys.ID_SUFFIX;

@Service
@Qualifier("Markdown")
public class MarkdownWikiRenderer implements IMarkupRenderer {
    final Renderer renderer;

    final TOCRenderService tocRenderService;

    public MarkdownWikiRenderer(
            @Autowired Renderer renderer,
            @Autowired TOCRenderService tocRenderService) {
        this.renderer = renderer;
        this.tocRenderService = tocRenderService;
    }

    @Override
    public RenderResult renderWithInfo(String markup, RenderContext renderContext) {
        List<Extension> extensions = Arrays.asList(TablesExtension.create(), new MacroExtension());
        org.commonmark.parser.Parser parser = org.commonmark.parser.Parser.builder()
                .extensions(extensions)
                .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
                .build();
        Node markdownTree = parser.parse(markup);

        ITreeNode node = translate(markup, markdownTree);
        RenderContext htmlContext =
                new RenderContext(
                        renderContext.site(),
                        renderContext.page(),
                        renderContext.user(),
                        this,
                        renderContext.renderState());
        String rendered = renderer.render(node, htmlContext);
        String toc = renderToC(renderContext);
        RenderContext plaintextContext =
                new RenderContext(
                        renderContext.site(),
                        renderContext.page(),
                        renderContext.user(),
                        this,
                        new HashMap<>());
        plaintextContext.renderState().put(PLAIN_TEXT, true);
        String plainText = renderer.renderPlaintext(node, plaintextContext);
        return new RenderResult(toc + rendered, plainText, renderContext.renderState());
    }

    @Override
    public String renderToString(String markup, RenderContext renderContext) {
        return renderWithInfo(markup, renderContext).renderedText();
    }

    @SuppressWarnings("unchecked")
    private String renderToC(RenderContext renderContext) {
        List<HeaderRef> headers =
                (List<HeaderRef>)
                        renderContext.renderState().getOrDefault(HEADERS, Collections.emptyList());
        Object forceTOC = renderContext.renderState().get(TOC);
        if (Boolean.FALSE.equals(forceTOC) || (headers.size() < 3) && !Boolean.TRUE.equals(forceTOC)) {
            return "";
        }
        String idSuffix = renderContext.renderState().getOrDefault(ID_SUFFIX, "").toString();
        return tocRenderService.renderTOC(headers, idSuffix);
    }

    private ITreeNode translate(String fullText, Node markdownTree) {
        LazerwikiVisitor visitor = new LazerwikiVisitor(fullText);
        markdownTree.accept(visitor);
        return visitor.getTree();
    }

    public static class LazerwikiVisitor implements Visitor {
        ITreeNode root;
        Stack<ContainerNode> nodeStack = new Stack<>();
        String fullText;
        ParseContext fullContext;

        public LazerwikiVisitor(String fullText) {
            this.fullText = fullText;
            fullContext = new ParseContext(fullText);
        }

        @Override
        public void visit(BlockQuote blockQuote) {

        }

        @Override
        public void visit(BulletList bulletList) {

        }

        @Override
        public void visit(Code code) {

        }

        @Override
        public void visit(Document document) {

            ContainerNode container = new ContainerNode();
            container.setParseContext(fullContext);
            container.setPosition(Pair.of(0, fullText.length()));
            root = container;
            nodeStack.push(container);
            childAccept(document);
            nodeStack.pop();
            next(document);
        }

        @Override
        public void visit(Emphasis emphasis) {

        }

        @Override
        public void visit(FencedCodeBlock fencedCodeBlock) {

        }

        @Override
        public void visit(HardLineBreak hardLineBreak) {

        }

        @Override
        public void visit(Heading heading) {
            HeaderNode node = new HeaderNode(heading.getLevel());
            node.setParseContext(fullContext);
            node.setPosition(getPosition(heading.getSourceSpans()));
            openNode().addChild(node);
            nodeStack.push(node);
            heading.getFirstChild().accept(this);
            nodeStack.pop();
            next(heading);
        }

        @Override
        public void visit(ThematicBreak thematicBreak) {

        }

        @Override
        public void visit(HtmlInline htmlInline) {

        }

        @Override
        public void visit(HtmlBlock htmlBlock) {

        }

        @Override
        public void visit(Image image) {

        }

        @Override
        public void visit(IndentedCodeBlock indentedCodeBlock) {

        }

        @Override
        public void visit(Link link) {
           LinkNode node = new LinkNode(link.getDestination());
           node.setParseContext(fullContext);
           node.setPosition(getPosition(link.getSourceSpans()));
           openNode().addChild(node);
           nodeStack.add(node);
           childAccept(link);
           nodeStack.pop();
           next(link);
        }

        @Override
        public void visit(ListItem listItem) {

        }

        @Override
        public void visit(OrderedList orderedList) {

        }

        @Override
        public void visit(Paragraph paragraph) {
            TaggedContainerNode node = new TaggedContainerNode(TaggedContainerNode.TYPE.PARAGRAPH);
            node.setParseContext(fullContext);
            node.setPosition(getPosition(paragraph.getSourceSpans()));
            openNode().addChild(node);
            nodeStack.push(node);
            childAccept(paragraph);
            nodeStack.pop();
            next(paragraph);
        }

        @Override
        public void visit(SoftLineBreak softLineBreak) {
            TextNode node = new TextNode("\n");
            node.setParseContext(fullContext);
            node.setPosition(getPosition(softLineBreak.getSourceSpans()));
            openNode().addChild(node);
            next(softLineBreak);
        }

        @Override
        public void visit(StrongEmphasis strongEmphasis) {

        }

        @Override
        public void visit(Text text) {
            StringBuilder content = new StringBuilder();
            Node node = text;
            List<SourceSpan> sourceSpans = new ArrayList<>();
            do {
                if (node instanceof Text) {
                    content.append(((Text)node).getLiteral());
                    sourceSpans.addAll(node.getSourceSpans());
                    node  = node.getNext();
                }
                else if (node instanceof SoftLineBreak) {
                    content.append("\n");
                    node  = node.getNext();
                }
                else {
                    break;
                }
            } while (node != null);
            TextNode textNode = new TextNode(content.toString());
            textNode.setParseContext(fullContext);
            textNode.setPosition(getPosition(sourceSpans));
            openNode().addChild(textNode);
            if (node != null) {
                node.accept(this);
            }
        }

        @Override
        public void visit(LinkReferenceDefinition linkReferenceDefinition) {

        }

        @Override
        public void visit(CustomBlock customBlock) {
            if (customBlock instanceof MacroBlock macroBlock) {
                MacroNode node = new MacroNode(macroBlock.getMacroText(), macroBlock.getMacroFullText());
                node.setParseContext(fullContext);
                node.setPosition(getPosition(macroBlock.getSourceSpans()));
                openNode().addChild(node);
            }
            next(customBlock);
        }

        @Override
        public void visit(CustomNode customNode) {

        }

        void next(Node node) {
            Node nextNode = node.getNext();
            if (nextNode != null) {
                nextNode.accept(this);
            }
        }

        void childAccept(Node node) {
            if (node.getFirstChild() != null) {
                node.getFirstChild().accept(this);
            }
        }

        ContainerNode openNode() {
            return nodeStack.peek();
        }

        Pair<Integer, Integer> getPosition(List<SourceSpan> sourceSpans) {
            int start = fullContext.translatePosition(sourceSpans.getFirst().getLineIndex(), sourceSpans.getFirst().getColumnIndex());
            int end = fullContext.translatePosition(sourceSpans.getLast().getLineIndex(), sourceSpans.getLast().getColumnIndex()) + sourceSpans.getLast().getLength() - 1;
            return Pair.of(start, end);
        }

        public ITreeNode getTree() {
            return root;
        }
    }

    public static class MacroExtension implements Parser.ParserExtension {

        @Override
        public void extend(Parser.Builder parserBuilder) {
            parserBuilder.customBlockParserFactory(new MacroBlockParser.Factory());
        }
    }

    public static class MacroBlock extends CustomBlock {
        private String macroText;
        private String macroFullText;

        public String getMacroText() {
            return macroText;
        }

        public String getMacroFullText() {
            return macroFullText;
        }
    }

    public static class MacroBlockParser extends AbstractBlockParser {
        static final String MACRO_START = "~~MACRO~~";
        static final String MACRO_END = "~~/MACRO~~";
        static final java.util.regex.Pattern MACRO_TOKEN =
                java.util.regex.Pattern.compile("~~(/)?MACRO~~");

        private final MacroBlock block = new MacroBlock();
        private final StringBuilder raw = new StringBuilder();
        private int macroCount = 0;
        private boolean done = false;

        @Override
        public Block getBlock() {
            return block;
        }

        @Override
        public BlockContinue tryContinue(ParserState parserState) {
            if (done) {
                return BlockContinue.none();
            }
            return BlockContinue.atIndex(parserState.getIndex());
        }

        @Override
        public void addLine(SourceLine line) {
            if (!raw.isEmpty()) {
                raw.append('\n');
            }
            String content = line.getContent().toString();
            java.util.regex.Matcher matcher = MACRO_TOKEN.matcher(content);
            while (matcher.find()) {
                if (matcher.group(1) == null) {
                    macroCount++;
                } else {
                    macroCount--;
                    if (macroCount == 0) {
                        raw.append(content, 0, matcher.end());
                        done = true;
                        return;
                    }
                }
            }
            raw.append(content);
        }

        @Override
        public void closeBlock() {
            String fullText = raw.toString();
            block.macroFullText = fullText;
            block.macroText =
                    done
                            ? fullText.substring(MACRO_START.length(), fullText.length() - MACRO_END.length())
                            : fullText;
        }

        public static class Factory extends AbstractBlockParserFactory {

            @Override
            public BlockStart tryStart(ParserState state, MatchedBlockParser matchedBlockParser) {
                CharSequence line = state.getLine().getContent();
                int nextNonSpace = state.getNextNonSpaceIndex();
                String content = line.toString();
                if (!content.startsWith(MACRO_START, nextNonSpace)) {
                    return BlockStart.none();
                }
                return BlockStart.of(new MacroBlockParser()).atIndex(nextNonSpace);
            }
        }
    }
}
