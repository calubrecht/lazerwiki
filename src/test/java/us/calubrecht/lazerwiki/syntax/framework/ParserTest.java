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
        Pair<Integer, Integer> tgtPos = l1.getTargetPosition();
        assertEquals(" link:Target", testText.substring(tgtPos.getLeft(), tgtPos.getRight() + 1));
        ITreeNode descNode = l1.getChildren().get(0);
        assertEquals("with a link ", descNode.getSourceFromContext());

        // Continue tests.
        // Images
        // Images and Links embedded deep in other structures
        // Lists
        // Tables
    }
}