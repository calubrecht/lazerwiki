package us.calubrecht.lazerwiki.syntax.framework;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.service.*;
import us.calubrecht.lazerwiki.syntax.nodes.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = { Parser.class, ParserTest.TestConfig.class})
@ActiveProfiles("test")
class ParserTest {
    @Autowired
    Parser underTest;

    @Configuration
    @ComponentScan({"us.calubrecht.lazerwiki.syntax"})
    public static class TestConfig {
    }

    @MockBean
    PageService pageService;

    @MockBean
    MacroService macroService;

    @MockBean
    RandomService randomService;

    @MockBean
    LinkOverrideService linkOverrideService;

    @MockBean
    MediaOverrideService mediaOverrideService;

    /**
     * Convenience casting function
     */
    ContainerNode cn(ITreeNode node) {
        return (ContainerNode)node;
    }

    @Test
    void testParse_testPositionHandling() {
        // Very basic, 2 paragraphs;
        ContainerNode nodes = cn(underTest.parse("This is one paragraph\n\nThis is another"));
        ContainerNode p0 = cn(nodes.getChildren().get(0));
        assertEquals("This is one paragraph\n\n", p0.getSourceFromContext());
        ContainerNode p1 = cn(nodes.getChildren().get(1));
        assertEquals("This is another", p1.getSourceFromContext());
    }

    @Test
    void testParse_testPositionHandling_Links() {
        // Link context
        String testText = "This is one paragraph\n\nThis is a paragraph [[ link:Target|with a link ]]. All goood\nAnd more";
        ContainerNode nodes = cn(underTest.parse(testText));
        assertEquals(2, nodes.getChildren().size());
        ContainerNode p0 = cn(nodes.getChildren().get(0));
        assertEquals("This is one paragraph\n\n", p0.getSourceFromContext());
        ContainerNode p1 = cn(nodes.getChildren().get(1));
        assertEquals("This is a paragraph [[ link:Target|with a link ]]. All goood\nAnd more", p1.getSourceFromContext());
        LinkNode l1 = (LinkNode) p1.getChildren().get(1);
        assertEquals("[[ link:Target|with a link ]]", l1.getSourceFromContext());
        assertEquals(" link:Target", l1.getTargetSourceFromContext());
        assertEquals(" link:Target", l1.getDest());
        ITreeNode descNode = l1.getChildren().get(0);
        assertEquals("with a link ", descNode.getSourceFromContext());
        testText = "This is one paragraph\n\nThis is a paragraph [[ link:Target|with a **bold link** ]]. All goood\nAnd more";
        nodes = cn(underTest.parse(testText));
        p1 = cn(nodes.getChildren().get(1));
        l1 = (LinkNode) p1.getChildren().get(1);
        assertEquals("with a ", l1.getChildren().get(0).getSourceFromContext());
        assertEquals("**bold link**", l1.getChildren().get(1).getSourceFromContext());
    }

    @Test
    void testParse_testPositionHandling_Images() {
        String imageInput = "This is one paragraph\n\nThis is a paragraph {{ image:target| title }}. All goood\nAnd more";
        ContainerNode nodes = cn(underTest.parse(imageInput));
        ImageNode imageNode = (ImageNode) cn(nodes.getChildren().get(1)).getChildren().get(1);
        assertEquals(2, nodes.getChildren().size());
        assertEquals("{{ image:target| title }}", imageNode.getSourceFromContext());
        assertEquals("image:target", imageNode.getSourceSourceFromContext());
    }

    @Test
    void testParse_testPositionHandling_Other() {
        // Lists
        String listInput = " - Simple List\n *List Changes Type\n   * DeepestList\n * and backout\n";
        ContainerNode nodes = cn(underTest.parse(listInput));
        assertEquals(listInput, nodes.getSourceFromContext());
        ListNode list1 = (ListNode)nodes.getChildren().get(1);
        assertEquals(3, list1.getItems().size());
        assertEquals(" *List Changes Type\n   * DeepestList\n * and backout", list1.getSourceFromContext());
        ListNode list1_1 = ((ListChild.ListChildList)list1.getItems().get(1)).list();
        assertEquals("   * DeepestList\n", list1_1.getSourceFromContext()); // Suspicious that newline sometimes is and sometimes isn't part of node

        String listInputWithLink = " - Simple List\n *List Changes Type\n   * DeepestList [[Link| with a link]]! \n";
        nodes = cn(underTest.parse(listInputWithLink));
        list1 = (ListNode)nodes.getChildren().get(1);
        list1_1 = ((ListChild.ListChildList)list1.getItems().get(1)).list();
        assertEquals("   * DeepestList [[Link| with a link]]! ", list1_1.getSourceFromContext()); // Suspicious that newline sometimes is and sometimes isn't part of node
        LinkNode link = (LinkNode)((ListChild.ListItemNode)list1_1.getItems().get(0)).getChildren().get(1);
        assertEquals("[[Link| with a link]]", link.getSourceFromContext());
        assertEquals("Link", link.getTargetSourceFromContext());
        ITreeNode descNode = link.getChildren().get(0);
        assertEquals(" with a link", descNode.getSourceFromContext());
        // Tables
        String inputSimpleTable = "|First|Line|\n|Second|Line|";
        nodes = cn(underTest.parse(inputSimpleTable));
        TableNode table = (TableNode)nodes.getChildren().get(0);
        assertEquals(inputSimpleTable, table.getSourceFromContext());
        assertEquals("|Second|Line|", (table.getChildren().get(1)).getSourceFromContext());
        assertEquals("|Second|", (cn(table.getChildren().get(1)).getChildren().get(0)).getSourceFromContext());

        String inputTableWithLink = "|First|Line|\n|Second|Cell [[Link| with a link]]|";
        nodes = cn(underTest.parse(inputTableWithLink));
        table = (TableNode)nodes.getChildren().get(0);
        assertEquals(inputTableWithLink, table.getSourceFromContext());
        assertEquals("|Cell [[Link| with a link]]|", (cn(table.getChildren().get(1)).getChildren().get(1)).getSourceFromContext());
        ContainerNode cell = cn((cn(table.getChildren().get(1)).getChildren().get(1)));
        link = (LinkNode)cell.getChildren().get(1);
        assertEquals("[[Link| with a link]]", link.getSourceFromContext());

        // Test hiddens
    }

    @Test
    public void testParse_testPositionFails() {
        LinkNode node = new LinkNode("OneDest");
        assertEquals(null, node.getTargetSourceFromContext());
        assertEquals(null, node.getSourceFromContext());
        assertThrows(RuntimeException.class, () -> Parser.validateNode(node));
        node.setParseContext(new ParseContext());
        assertEquals(null, node.getTargetSourceFromContext());
        assertEquals(null, node.getSourceFromContext());
        assertThrows(RuntimeException.class, () -> Parser.validateNode(node));
        LinkNode node2 = new LinkNode("OneDest");
        node2.setPosition(1, 5);
        assertEquals(null, node2.getTargetSourceFromContext());
        assertEquals(null, node2.getSourceFromContext());
        assertThrows(RuntimeException.class, () -> Parser.validateNode(node2));

        ImageNode imageNode = new ImageNode("img.jpg", "", "", null);
        assertEquals(null, imageNode.getSourceSourceFromContext());
        imageNode.setParseContext(new ParseContext());
        assertEquals(null, imageNode.getSourceSourceFromContext());
        imageNode = new ImageNode("img.jpg", "", "", null);
        imageNode.setPosition(1, 5);
        assertEquals(null, imageNode.getSourceSourceFromContext());
    }

    @Test
    void testParseList() {
        String listInput = " - Simple List\n *List Changes Type\n   * DeepestList\n * and backout\n";
        ContainerNode nodes = cn(underTest.parse(listInput));
        // 2 top level lists
        assertEquals(2, nodes.getChildren().size());
        assertEquals(ListNode.LIST_TYPE.ORDERED, ((ListNode)nodes.getChildren().get(0)).getListType());
        assertEquals(ListNode.LIST_TYPE.UNORDERED, ((ListNode)nodes.getChildren().get(1)).getListType());
        ListNode l1 = (ListNode)nodes.getChildren().get(1);
        assertEquals(3, l1.getItems().size());
        ListNode l1_1 = ((ListChild.ListChildList)l1.getItems().get(1)).list();
        assertEquals(ListNode.LIST_TYPE.UNORDERED, l1_1.getListType());

        String mixedInnerListInput = " - Simple List\n *List Changes Type\n   - DeepestList\n * and backout\n";
        nodes = cn(underTest.parse(mixedInnerListInput));
        l1 = (ListNode)nodes.getChildren().get(1);
        assertEquals(3, l1.getItems().size());
        l1_1 = ((ListChild.ListChildList)l1.getItems().get(1)).list();
        assertEquals(ListNode.LIST_TYPE.ORDERED, l1_1.getListType());
    }

    void verifySpans(TableNode.TableCellNode cell, int colSpan, int rowSpan) {
        assertEquals(colSpan, cell.getColSpan());
        assertEquals(rowSpan, cell.getRowSpan());
    }

    @Test
    void testParseTable() {
        String inputSimpleTable = "|First|Line|\n|Second|Line|";
        ContainerNode nodes = cn(underTest.parse(inputSimpleTable));
        // 1 table
        assertEquals(1, nodes.getChildren().size());
        TableNode table = (TableNode)nodes.getChildren().get(0);
        // 2x2 table
        assertEquals(2, table.getChildren().size());
        assertEquals(2, cn(table.getChildren().get(0)).getChildren().size());
        assertEquals(2, cn(table.getChildren().get(1)).getChildren().size());

        String tableWithColSpan = "^Header|Line|\n|Second||";
        nodes = cn(underTest.parse(tableWithColSpan));
        // 1 table
        assertEquals(1, nodes.getChildren().size());
        table = (TableNode)nodes.getChildren().get(0);
        // 2nd row is missing 1 cell, 1st cell has col span=2
        assertEquals(2, table.getChildren().size());
        assertEquals(2, cn(table.getChildren().get(0)).getChildren().size());
        assertEquals(1, cn(table.getChildren().get(1)).getChildren().size());
        verifySpans((TableNode.TableCellNode)cn(table.getChildren().get(1)).getChildren().get(0), 2, 1);
        // 1st row's cells still rowspan 1
        verifySpans((TableNode.TableCellNode)cn(table.getChildren().get(0)).getChildren().get(0), 1, 1);
        verifySpans((TableNode.TableCellNode)cn(table.getChildren().get(0)).getChildren().get(1), 1, 1);


        String tableWithRowSpan = "|One|Two|\n|Three| :: |";
        nodes = cn(underTest.parse(tableWithRowSpan));
        // 1 table
        assertEquals(1, nodes.getChildren().size());
        table = (TableNode)nodes.getChildren().get(0);
        // 2nd row is missing 1 cell. 2nd cell in 1st row has rowspan 2
        assertEquals(2, table.getChildren().size());
        assertEquals(2, cn(table.getChildren().get(0)).getChildren().size());
        assertEquals(1, cn(table.getChildren().get(1)).getChildren().size());
        verifySpans((TableNode.TableCellNode)cn(table.getChildren().get(0)).getChildren().get(1), 1, 2);
        // Other cells still have correct span
        verifySpans((TableNode.TableCellNode)cn(table.getChildren().get(0)).getChildren().get(0), 1, 1);
        verifySpans((TableNode.TableCellNode)cn(table.getChildren().get(1)).getChildren().get(0), 1, 1);
    }

    @Test
    public void testParse_UnnaturalCases() {
        ParseContext parseContext = new ParseContext("Some Text");
        ContainerNode container = new ContainerNode();
        container.setParseContext(parseContext);
        container.setPosition(0,0);
        underTest.parse(parseContext, container, List.of());
        assertEquals(1, container.getChildren().size());
        TextNode tn = (TextNode)container.getChildren().get(0);
        assertEquals("Some Text", tn.asString());
    }

    static class InvalidNode extends AbstractNode {

    }

    @Test
    public void testInvalidNode() {
        InvalidNode node = new InvalidNode();
        // InvalidNode does not implement a valid asString() method. Avoid
        // It being used raw in a renderer
        assertThrows(UnsupportedOperationException.class, () -> node.asString());

    }

    @Test
    public void testDefaultRenderer() {
        // Default Renderer does not attach directly to any node types
        assertEquals(List.of(), underTest.parserRegistrar.DEFAULT_RENDERER.getTargets());

    }
}