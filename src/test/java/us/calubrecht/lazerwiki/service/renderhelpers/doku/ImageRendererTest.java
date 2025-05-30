package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import us.calubrecht.lazerwiki.model.LinkOverride;
import us.calubrecht.lazerwiki.model.LinkOverrideInstance;
import us.calubrecht.lazerwiki.model.MediaOverride;
import us.calubrecht.lazerwiki.service.MediaOverrideService;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static us.calubrecht.lazerwiki.model.RenderResult.RENDER_STATE_KEYS.OVERRIDE_STATS;

class ImageRendererTest {

    final ImageRenderer renderer = new ImageRenderer();
    MediaOverrideService mediaOverrideService = Mockito.mock(MediaOverrideService.class);

    @BeforeEach
    void setup() throws NoSuchFieldException, IllegalAccessException {
        Field uieField = ImageRenderer.class.getDeclaredField("unscalableImageExts");
        uieField.setAccessible(true);
        uieField.set(renderer, Set.of("avif"));
        Field mediaOverrideServiceFld = ImageRenderer.class.getDeclaredField("mediaOverrideService");
        mediaOverrideServiceFld.setAccessible(true);
        mediaOverrideServiceFld.set(renderer, mediaOverrideService);
    }

    @Test
    void parseInnerAlignment() {
        String input1 = "img.jpg";

        RenderContext renderContext = new RenderContext("host", "site", "page", "user");
        assertEquals(
                "<img src=\"/_media/img.jpg\" class=\"media\" loading=\"lazy\">",
                renderer.parseInner(input1, null, renderContext).toString()
        );
        String inputCentered = " img.jpg ";
        assertEquals(
                "<img src=\"/_media/img.jpg\" class=\"mediacenter\" loading=\"lazy\">",
                renderer.parseInner(inputCentered, null, renderContext).toString()
        );
        String inputRight = " img.jpg";
        assertEquals(
                "<img src=\"/_media/img.jpg\" class=\"mediaright\" loading=\"lazy\">",
                renderer.parseInner(inputRight, null, renderContext).toString()
        );
        String inputLeft = "img.jpg ";
        assertEquals(
                "<img src=\"/_media/img.jpg\" class=\"medialeft\" loading=\"lazy\">",
                renderer.parseInner(inputLeft, null, renderContext).toString()
        );

    }

    @Test
    void applyInlineStyles() {
        String justWidth = "img.avif?30";
        RenderContext renderContext = new RenderContext("host", "site", "page", "user");
        assertEquals(
                "<img src=\"/_media/img.avif?30\" class=\"media\" style=\"width:30px\" loading=\"lazy\">",
                renderer.parseInner(justWidth, null, renderContext).toString()
        );

        String widthAndHeight = "img.avif?30x30";
        assertEquals(
                "<img src=\"/_media/img.avif?30x30\" class=\"media\" style=\"width:30px; height:30px\" loading=\"lazy\">",
                renderer.parseInner(widthAndHeight, null, renderContext).toString()
        );


        String justHeight = "img.avif?0x30";
        assertEquals(
                "<img src=\"/_media/img.avif?0x30\" class=\"media\" style=\"height:30px\" loading=\"lazy\">",
                renderer.parseInner(justHeight, null, renderContext).toString()
        );

        String noSize = "img.avif";
        assertEquals(
                "<img src=\"/_media/img.avif\" class=\"media\" loading=\"lazy\">",
                renderer.parseInner(noSize, null, renderContext).toString()
        );

        // ignore non-size
        String nonSize = "img.avif?soWhat";
        assertEquals(
                "<img src=\"/_media/img.avif\" class=\"media\" loading=\"lazy\">",
                renderer.parseInner(nonSize, null, renderContext).toString()
        );

        String sizeAndnonsize = "img.avif?soWhat&20";
        assertEquals(
                "<img src=\"/_media/img.avif?20\" class=\"media\" style=\"width:20px\" loading=\"lazy\">",
                renderer.parseInner(sizeAndnonsize, null, renderContext).toString()
        );
    }

    @Test
    public void testOverrides() {
        DokuwikiParser.ImageContext tree = Mockito.mock(DokuwikiParser.ImageContext.class);
        DokuwikiParser.Inner_textContext innerText = Mockito.mock(DokuwikiParser.Inner_textContext.class);
        Token startToken = Mockito.mock(Token.class);
        when(tree.inner_text(0)).thenReturn(innerText);
        when(innerText.getStart()).thenReturn(startToken);
        when(innerText.getText()).thenReturn("img.jpg");
        when(startToken.getStartIndex()).thenReturn(10);
        String input1 = "img.jpg";
        List<MediaOverride> overrides = List.of(
                new MediaOverride("default", "", "page", "", "img.jpg", "ns2", "img5.jpg"));
        when(mediaOverrideService.getOverrides("host", "page")).thenReturn(
                overrides
        );

        RenderContext renderContext = new RenderContext("host", "site", "page", "user");
        assertEquals(
                "<img src=\"/_media/ns2:img5.jpg\" class=\"media\" loading=\"lazy\">",
                renderer.parseInner(input1, tree, renderContext).toString()
        );
        List<LinkOverrideInstance> overrideStats = (List<LinkOverrideInstance>) renderContext.renderState().get(OVERRIDE_STATS.name());
        assertEquals(1, overrideStats.size());
        LinkOverrideInstance override = overrideStats.get(0);
        assertEquals("img.jpg", override.src());
        assertEquals("ns2:img5.jpg", override.override());
        assertEquals(10, override.start());
        assertEquals(17, override.stop());
    }

    @Test
    public void testOverridesWithComplexInputs() {
        DokuwikiParser.ImageContext tree = Mockito.mock(DokuwikiParser.ImageContext.class);
        DokuwikiParser.Inner_textContext innerText = Mockito.mock(DokuwikiParser.Inner_textContext.class);
        Token startToken = Mockito.mock(Token.class);
        when(tree.inner_text(0)).thenReturn(innerText);
        when(innerText.getStart()).thenReturn(startToken);
        when(innerText.getText()).thenReturn(" img.jpg | alternate text");
        when(startToken.getStartIndex()).thenReturn(10);
        String input1 = " img.jpg";
        List<MediaOverride> overrides = List.of(
                new MediaOverride("default", "", "page", "", "img.jpg", "ns2", "img5.jpg"));
        when(mediaOverrideService.getOverrides("host", "page")).thenReturn(
                overrides
        );

        RenderContext renderContext = new RenderContext("host", "site", "page", "user");
        assertEquals(
                "<img src=\"/_media/ns2:img5.jpg\" class=\"mediaright\" loading=\"lazy\">",
                renderer.parseInner(input1, tree, renderContext).toString()
        );
        List<LinkOverrideInstance> overrideStats = (List<LinkOverrideInstance>) renderContext.renderState().get(OVERRIDE_STATS.name());
        assertEquals(1, overrideStats.size());
        LinkOverrideInstance override = overrideStats.get(0);
        assertEquals("img.jpg", override.src());
        assertEquals("ns2:img5.jpg", override.override());
        assertEquals(11, override.start());
        assertEquals(18, override.stop());
    }
}