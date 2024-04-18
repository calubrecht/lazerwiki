package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.junit.jupiter.api.Test;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

import static org.junit.jupiter.api.Assertions.*;

class ImageRendererTest {

    final ImageRenderer renderer = new ImageRenderer();

    @Test
    void parseInnerAlignment() {
        String input1 = "img.jpg";
        RenderContext renderContext = new RenderContext("host", "site", "user");
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
}