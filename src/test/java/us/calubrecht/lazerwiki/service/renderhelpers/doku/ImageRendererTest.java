package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImageRendererTest {

    ImageRenderer renderer = new ImageRenderer();

    @Test
    void parseInnerAlignment() {
        String input1 = "img.jpg";
        assertEquals(
                "<img src=\"/_media/img.jpg\" class=\"media\" loading=\"lazy\">",
                renderer.parseInner(input1).toString()
        );
        String inputCentered = " img.jpg ";
        assertEquals(
                "<img src=\"/_media/img.jpg\" class=\"mediacenter\" loading=\"lazy\">",
                renderer.parseInner(inputCentered).toString()
        );
        String inputRight = " img.jpg";
        assertEquals(
                "<img src=\"/_media/img.jpg\" class=\"mediaright\" loading=\"lazy\">",
                renderer.parseInner(inputRight).toString()
        );
        String inputLeft = "img.jpg ";
        assertEquals(
                "<img src=\"/_media/img.jpg\" class=\"medialeft\" loading=\"lazy\">",
                renderer.parseInner(inputLeft).toString()
        );

    }
}