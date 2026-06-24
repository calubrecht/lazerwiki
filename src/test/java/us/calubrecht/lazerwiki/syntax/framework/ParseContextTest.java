package us.calubrecht.lazerwiki.syntax.framework;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import org.junit.jupiter.api.Test;

class ParseContextTest {

  @Test
  void test_remainingLine() {
    ParseContext context = new ParseContext("OneLine\nOr two\nThree");

    assertEquals("OneLine", context.peekLine());
    assertEquals("OneLine", context.remainingLine());
    context.advanceChars(2);
    assertEquals("OneLine", context.peekLine());
    assertEquals("eLine", context.remainingLine());
    assertEquals("OneLine", context.toString());
  }

  @Test
  public void test_isEmpty() {
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
  }

  @Test
  public void test_readOnly() {
    ParseContext context = new ParseContext();
    context.addLine("One Line");
    context.addLine("Two Line");
    context.lock();
    ParseContext root = new ParseContext();
    assertThrows(UnsupportedOperationException.class, () -> context.addLine("Three Line"));
    assertThrows(UnsupportedOperationException.class, () -> context.setRoot(root, 5));
  }

  @Test
  public void test_iterable() {
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
