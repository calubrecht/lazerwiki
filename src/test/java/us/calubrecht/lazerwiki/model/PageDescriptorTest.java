package us.calubrecht.lazerwiki.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PageDescriptorTest {

    @Test
    public void testRenderedName() {
        PageDescriptor plane = new PageDescriptor("", "basic");
        assertEquals("Basic", plane.renderedName());

        PageDescriptor withUnderscore = new PageDescriptor("", "basic_plus");
        assertEquals("Basic Plus", withUnderscore.renderedName());

        PageDescriptor withCamelCase = new PageDescriptor("", "BigAndImportant");
        assertEquals("Big And Important", withCamelCase.renderedName());

        PageDescriptor withCamelCaseAndUnderscore = new PageDescriptor("", "Big_AndImportant");
        assertEquals("Big And Important", withCamelCaseAndUnderscore.renderedName());

        PageDescriptor withMultipleCaps = new PageDescriptor("", "AIIsImportant");
        assertEquals("A I Is Important", withMultipleCaps.renderedName());

        PageDescriptor homepage = new PageDescriptor("", "");
        assertEquals("Home", homepage.renderedName());

        PageDescriptor pageWithNums = new PageDescriptor("", "newPage234a");
        assertEquals("New Page 234 A", pageWithNums.renderedName());
    }

    @Test
    public void testToString() {
        PageDescriptor pd  = new PageDescriptor("", "WithoutNS");
        assertEquals("WithoutNS", pd.toString());
        pd  = new PageDescriptor("with:big:ns", "page");
        assertEquals("with:big:ns:page", pd.toString());

    }

    @Test
    public void fromFullName() {
        PageDescriptor pd = PageDescriptor.fromFullName("WithoutNS");
        assertEquals("", pd.namespace());
        assertEquals("WithoutNS", pd.pageName());

        pd = PageDescriptor.fromFullName("with:big:ns:page");
        assertEquals("with:big:ns", pd.namespace());
        assertEquals("page", pd.pageName());
    }
}
