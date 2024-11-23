package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ImageRendererTest {

    final ImageRenderer renderer = new ImageRenderer();

    @BeforeEach
    void setup() throws NoSuchFieldException, IllegalAccessException {
        Field uieField = ImageRenderer.class.getDeclaredField("unscalableImageExts");
        uieField.setAccessible(true);
        uieField.set(renderer, Set.of("avif"));
    }

    @Test
    void parseInnerAlignment() {
        String input1 = "img.jpg";
        RenderContext renderContext = new RenderContext("host", "site", "page", "user");
        assertEquals(
                "<img src=\"/_media/img.jpg\" class=\"media\" loading=\"lazy\">",
                renderer.parseInner(input1, renderContext).toString()
        );
        String inputCentered = " img.jpg ";
        assertEquals(
                "<img src=\"/_media/img.jpg\" class=\"mediacenter\" loading=\"lazy\">",
                renderer.parseInner(inputCentered, renderContext).toString()
        );
        String inputRight = " img.jpg";
        assertEquals(
                "<img src=\"/_media/img.jpg\" class=\"mediaright\" loading=\"lazy\">",
                renderer.parseInner(inputRight, renderContext).toString()
        );
        String inputLeft = "img.jpg ";
        assertEquals(
                "<img src=\"/_media/img.jpg\" class=\"medialeft\" loading=\"lazy\">",
                renderer.parseInner(inputLeft, renderContext).toString()
        );

    }

    @Test
    void applyInlineStyles() {
        String justWidth = "img.avif?30";
        RenderContext renderContext = new RenderContext("host", "site", "page", "user");
        assertEquals(
                "<img src=\"/_media/img.avif?30\" class=\"media\" style=\"width:30px\" loading=\"lazy\">",
                renderer.parseInner(justWidth, renderContext).toString()
        );

        String widthAndHeight = "img.avif?30x30";
        assertEquals(
                "<img src=\"/_media/img.avif?30x30\" class=\"media\" style=\"width:30px; height:30px\" loading=\"lazy\">",
                renderer.parseInner(widthAndHeight, renderContext).toString()
        );


        String justHeight = "img.avif?0x30";
        assertEquals(
                "<img src=\"/_media/img.avif?0x30\" class=\"media\" style=\"height:30px\" loading=\"lazy\">",
                renderer.parseInner(justHeight, renderContext).toString()
        );

        String noSize = "img.avif";
        assertEquals(
                "<img src=\"/_media/img.avif\" class=\"media\" loading=\"lazy\">",
                renderer.parseInner(noSize, renderContext).toString()
        );

        // ignore non-size
        String nonSize = "img.avif?soWhat";
        assertEquals(
                "<img src=\"/_media/img.avif\" class=\"media\" loading=\"lazy\">",
                renderer.parseInner(nonSize, renderContext).toString()
        );

        String sizeAndnonsize = "img.avif?soWhat&20";
        assertEquals(
                "<img src=\"/_media/img.avif?20\" class=\"media\" style=\"width:20px\" loading=\"lazy\">",
                renderer.parseInner(sizeAndnonsize, renderContext).toString()
        );
    }
}