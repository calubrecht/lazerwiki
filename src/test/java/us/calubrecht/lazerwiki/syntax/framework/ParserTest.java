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
import us.calubrecht.lazerwiki.syntax.nodes.ContainerNode;
import us.calubrecht.lazerwiki.syntax.nodes.LinkNode;
import us.calubrecht.lazerwiki.syntax.nodes.ListChild;
import us.calubrecht.lazerwiki.syntax.nodes.ListNode;

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
     * Conveniance casting function
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

        // Link context
        String testText = "This is one paragraph\n\nThis is a paragraph [[ link:Target|with a link ]]. All goood\nAnd more";
        nodes = cn(underTest.parse(testText));
        assertEquals(2, nodes.getChildren().size());
        p0 = cn(nodes.getChildren().get(0));
        assertEquals("This is one paragraph\n\n", p0.getSourceFromContext());
        p1 = cn(nodes.getChildren().get(1));
        assertEquals("This is a paragraph [[ link:Target|with a link ]]. All goood\nAnd more", p1.getSourceFromContext());
        LinkNode l1 = (LinkNode)p1.getChildren().get(1);
        assertEquals("[[ link:Target|with a link ]]", l1.getSourceFromContext());
        assertEquals(" link:Target", l1.getTargetSourceFromContext());
        ITreeNode descNode = l1.getChildren().get(0);
        assertEquals("with a link ", descNode.getSourceFromContext());

        // Continue tests.
        // Images
        // Images and Links embedded deep in other structures
        // Lists
        String listInput = " - Simple List\n *List Changes Type\n   * DeepestList\n * and backout\n";
        nodes = cn(underTest.parse(listInput));
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
        descNode = link.getChildren().get(0);
        assertEquals(" with a link", descNode.getSourceFromContext());
        // Tables
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
}