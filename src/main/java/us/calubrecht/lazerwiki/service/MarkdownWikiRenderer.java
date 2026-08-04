package us.calubrecht.lazerwiki.service;

import org.apache.commons.lang3.tuple.Pair;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.*;
import org.commonmark.parser.IncludeSourceSpans;
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
        List<Extension> extensions = Arrays.asList(TablesExtension.create());
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
            document.getFirstChild().accept(this);
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
            node.setPosition(getPosition(heading.getSourceSpans().getFirst()));
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
           node.setPosition(getPosition(link.getSourceSpans().getFirst()));
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
            node.setPosition(getPosition(paragraph.getSourceSpans().getFirst()));
            openNode().addChild(node);
            nodeStack.push(node);
            paragraph.getFirstChild().accept(this);
            nodeStack.pop();
            next(paragraph);
        }

        @Override
        public void visit(SoftLineBreak softLineBreak) {

        }

        @Override
        public void visit(StrongEmphasis strongEmphasis) {

        }

        @Override
        public void visit(Text text) {
            TextNode node = new TextNode(text.getLiteral());
            node.setParseContext(fullContext);
            node.setPosition(getPosition(text.getSourceSpans().getFirst()));
            openNode().addChild(node);
            next(text);
        }

        @Override
        public void visit(LinkReferenceDefinition linkReferenceDefinition) {

        }

        @Override
        public void visit(CustomBlock customBlock) {

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

        Pair<Integer, Integer> getPosition(SourceSpan sourceSpan) {
            int start = fullContext.translatePosition(sourceSpan.getLineIndex(), sourceSpan.getColumnIndex());
            int end = start + sourceSpan.getLength() - 1;
            return Pair.of(start, end);
        }

        public ITreeNode getTree() {
            return root;
        }
    }
}
