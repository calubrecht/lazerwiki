package us.calubrecht.lazerwiki.syntax.framework;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;

import static org.junit.jupiter.api.Assertions.*;

class ParseContextTest {

    @Test
    void remainingLine() {
        ParseContext context = new ParseContext("OneLine\nOr two\nThree");

        assertEquals("OneLine", context.peekLine());
        assertEquals("OneLine", context.remainingLine());
        context.advanceChars(2);
        assertEquals("OneLine", context.peekLine());
        assertEquals("eLine", context.remainingLine());
        assertEquals("OneLine", context.toString());
    }

    @Test
    public void testIsEmpty() {
        ParseContext blank = new ParseContext();
        assertTrue(blank.isEmpty());
        ParseContext context = new ParseContext("OneLine\nOr two\nThree");
        context.advanceLine();
        context.advanceLine();
        assertFalse(context.isEmpty());
        context.advanceLine();
        assertTrue(context.isEmpty());
        context.advanceLine();
        assertTrue(context.isEmpty());

        ParseContext context2 = new ParseContext("One");
        assertFalse(context2.isEmpty());
        context2.advanceChars(3);
        assertTrue(context2.isEmpty());

        ParseContext context3 = new ParseContext("One\nTwo");
        assertFalse(context3.isEmpty());
        context3.advanceChars(3);
        assertFalse(context3.isEmpty());

        //	    public boolean isEmpty() {
        //129	7/8	        return nextLine == null || lineIdx >= lines.size() || (lineIdx == lines.size() -1) && charInLine >= nextLine.length();
        //130		    }
    }

    @Test
    public void testReadOnly() {
        ParseContext context = new ParseContext();
        context.addLine("One Line");
        context.addLine("Two Line");
        context.lock();
        ParseContext root = new ParseContext();
        assertThrows(UnsupportedOperationException.class, () -> context.addLine("Three Line"));
        assertThrows(UnsupportedOperationException.class, () -> context.setRoot(root, 5));

    }

    @Test
    public void testIterable() {
        ParseContext context = new ParseContext("OneLine\nOr two\nThree");
        List<String> items = new ArrayList<>();
        context.forEach(items::add);
        assertEquals(3, items.size());

        Spliterator<String> spliterator = context.spliterator();
        List<String> items2 = new ArrayList<>();
        spliterator.trySplit().forEachRemaining(items2::add);
        assertEquals(3, items2.size());
    }

}