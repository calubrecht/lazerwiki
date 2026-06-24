package us.calubrecht.lazerwiki.syntax.renderer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static us.calubrecht.lazerwiki.model.RenderResult.RenderStateKeys.OVERRIDE_STATS;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import us.calubrecht.lazerwiki.model.LinkOverrideInstance;
import us.calubrecht.lazerwiki.model.MediaOverride;
import us.calubrecht.lazerwiki.service.MediaOverrideService;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ParseContext;
import us.calubrecht.lazerwiki.syntax.nodes.ImageNode;

class ImageRendererTest {

  final MediaOverrideService mediaOverrideService = Mockito.mock(MediaOverrideService.class);
  final ImageRenderer renderer = new ImageRenderer(mediaOverrideService);

  @BeforeEach
  void setup() throws NoSuchFieldException, IllegalAccessException {
    Field uieField = ImageRenderer.class.getDeclaredField("unscalableImageExts");
    uieField.setAccessible(true);
    uieField.set(renderer, Set.of("avif"));
  }

  /**
   * Certain image files cannot be scaled on the backend. If these are provided with a size
   * descriptor, scale by using inline css styling.
   */
  @Test
  void test_applyInlineStylesForUnscalable() {
    String noScale = "";
    ImageNode imageNode = new ImageNode("img.avif", null, noScale, ImageNode.AlignType.NONE);
    RenderContext renderContext = new RenderContext("host", "site", "page", "user");
    assertEquals(
        "<img src=\"/_media/img.avif\" class=\"media\" loading=\"lazy\">",
        renderer.renderHtml(imageNode, renderContext).toString());

    String justWidth = "30";
    imageNode = new ImageNode("img.avif", null, justWidth, ImageNode.AlignType.NONE);
    assertEquals(
        "<img src=\"/_media/img.avif?30\" class=\"media\" style=\"width:30px\" loading=\"lazy\">",
        renderer.renderHtml(imageNode, renderContext).toString());

    String widthAnd0Height = "30x0";
    imageNode = new ImageNode("img.avif", null, widthAnd0Height, ImageNode.AlignType.NONE);
    assertEquals(
        "<img src=\"/_media/img.avif?30x0\" class=\"media\" style=\"width:30px\" loading=\"lazy\">",
        renderer.renderHtml(imageNode, renderContext).toString());

    String widthAndHeight = "30x30";
    imageNode = new ImageNode("img.avif", null, widthAndHeight, ImageNode.AlignType.NONE);
    assertEquals(
        "<img src=\"/_media/img.avif?30x30\" class=\"media\" style=\"width:30px; height:30px\" loading=\"lazy\">",
        renderer.renderHtml(imageNode, renderContext).toString());

    String justHeight = "0x30";
    imageNode = new ImageNode("img.avif", null, justHeight, ImageNode.AlignType.NONE);
    assertEquals(
        "<img src=\"/_media/img.avif?0x30\" class=\"media\" style=\"height:30px\" loading=\"lazy\">",
        renderer.renderHtml(imageNode, renderContext).toString());

    String sizeAndnonsize = "soWhat&20";
    imageNode = new ImageNode("img.avif", null, sizeAndnonsize, ImageNode.AlignType.NONE);
    assertEquals(
        "<img src=\"/_media/img.avif?20\" class=\"media\" style=\"width:20px\" loading=\"lazy\">",
        renderer.renderHtml(imageNode, renderContext).toString());

    String widthAndHeightContains = "c30x30";
    imageNode = new ImageNode("img.avif", null, widthAndHeightContains, ImageNode.AlignType.NONE);
    assertEquals(
        "<img src=\"/_media/img.avif?c30x30\" class=\"media\" style=\"width:30px; height:30px\" loading=\"lazy\">",
        renderer.renderHtml(imageNode, renderContext).toString());

    /*
    If image is of a scalable type, do not apply inline styles
     */
    imageNode = new ImageNode("scalable.jpg", null, widthAndHeight, ImageNode.AlignType.NONE);
    assertEquals(
        "<img src=\"/_media/scalable.jpg?30x30\" class=\"media\" loading=\"lazy\">",
        renderer.renderHtml(imageNode, renderContext).toString());
  }

  @Test
  void test_parseOptions() {
    assertEquals(Map.of(), renderer.parseOptions("", "a.jpg"));
    assertEquals(Map.of("size", "?30"), renderer.parseOptions("30", "a.jpg"));
    assertEquals(Map.of("size", "?30x30"), renderer.parseOptions("30x30", "a.jpg"));
    assertEquals(Map.of("size", "?0x30"), renderer.parseOptions("0x30", "a.jpg"));
    assertEquals(Map.of(), renderer.parseOptions("sowhat", "a.jpg"));
    assertEquals(Map.of("size", "?20"), renderer.parseOptions("soWhat&20", "a.jpg"));
    assertEquals(
        Map.of("size", "?20", "linkType", "full"), renderer.parseOptions("fullLink&20", "a.jpg"));
    assertEquals(
        Map.of("size", "?20", "linkType", "linkonly"),
        renderer.parseOptions("linkonly&20", "a.jpg"));
  }

  @Test
  void test_fileNameWithSpace() {
    ImageNode imageNode = new ImageNode("img 1.jpg?30", null, "", ImageNode.AlignType.NONE);
    RenderContext renderContext = new RenderContext("host", "site", "page", "user");
    assertEquals(
        "<img src=\"/_media/img 1.jpg?30\" class=\"media\" loading=\"lazy\">",
        renderer.renderHtml(imageNode, renderContext).toString());
  }

  @Test
  public void test_overrides() {
    String input1 = "img.jpg";
    List<MediaOverride> overrides =
        List.of(new MediaOverride("default", "", "page", "", "img.jpg", "ns2", "img5.jpg"));
    when(mediaOverrideService.getOverrides("host", "page")).thenReturn(overrides);

    ParseContext parseContext = new ParseContext("0123456789{{img.jpg}}");
    ImageNode imageNode = new ImageNode(input1, null, "", ImageNode.AlignType.NONE);
    imageNode.setPosition(10, 20);
    imageNode.setSourcePosition(Pair.of(12, 18));
    imageNode.setParseContext(parseContext);

    String imgString = imageNode.getSourceFromContext();
    assertEquals("{{img.jpg}}", imgString);
    String srcString = imageNode.getSourceSourceFromContext();
    assertEquals("img.jpg", srcString);
    RenderContext renderContext = new RenderContext("host", "site", "page", "user");
    assertEquals(
        "<img src=\"/_media/ns2:img5.jpg\" class=\"media\" loading=\"lazy\">",
        renderer.renderHtml(imageNode, renderContext).toString());
    @SuppressWarnings("unchecked")
    List<LinkOverrideInstance> overrideStats =
        (List<LinkOverrideInstance>) renderContext.renderState().get(OVERRIDE_STATS.name());
    assertEquals(1, overrideStats.size());
    LinkOverrideInstance override = overrideStats.get(0);
    assertEquals("img.jpg", override.src());
    assertEquals("ns2:img5.jpg", override.override());
    assertEquals(12, override.start()); // imageNode starts at 10, source string starts at 12
    assertEquals(19, override.stop());
  }

  @Test
  public void test_overridesWithComplexInputs() {
    String input1 = "img.jpg";
    ImageNode imageNode = new ImageNode(input1, " alternate text", "", ImageNode.AlignType.RIGHT);
    imageNode.setPosition(8, 21);
    imageNode.setSourcePosition(Pair.of(11, 17));
    List<MediaOverride> overrides =
        List.of(new MediaOverride("default", "", "page", "", "img.jpg", "ns2", "img5.jpg"));
    when(mediaOverrideService.getOverrides("host", "page")).thenReturn(overrides);

    RenderContext renderContext = new RenderContext("host", "site", "page", "user");
    assertEquals(
        "<img src=\"/_media/ns2:img5.jpg\" class=\"mediaright\" title=\" alternate text\" loading=\"lazy\">",
        renderer.renderHtml(imageNode, renderContext).toString());
    @SuppressWarnings("unchecked")
    List<LinkOverrideInstance> overrideStats =
        (List<LinkOverrideInstance>) renderContext.renderState().get(OVERRIDE_STATS.name());
    assertEquals(1, overrideStats.size());
    LinkOverrideInstance override = overrideStats.get(0);
    assertEquals("img.jpg", override.src());
    assertEquals("ns2:img5.jpg", override.override());
    assertEquals(11, override.start());
    assertEquals(18, override.stop());
  }
}
