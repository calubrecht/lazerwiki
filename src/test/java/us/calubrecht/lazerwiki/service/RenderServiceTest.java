package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.Page;
import us.calubrecht.lazerwiki.model.PageData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {RenderService.class})
@ActiveProfiles("test")
public class RenderServiceTest {

    @Autowired
    RenderService underTest;

    @MockBean
    PageService pageService;

    @MockBean
    IMarkupRenderer renderer;

    @Test
    public void testRender() {
        PageData pd = new PageData(null, "This is raw page text", true);
        when(renderer.render(eq("This is raw page text"))).thenReturn("This is Rendered Text");
        when(pageService.getPageData(any(), eq("ns:realPage"), any())).thenReturn(pd);

        assertEquals(new PageData("This is Rendered Text", "This is raw page text", true), underTest.getRenderedPage("host1", "ns:realPage", "Bob"));

        PageData noPageData = new PageData("Doesn't exist", "This is raw page text", false);
        when(pageService.getPageData(any(), eq("ns:nonPage"), any())).thenReturn(noPageData);
        assertEquals(new PageData("Doesn't exist", "This is raw page text", false), underTest.getRenderedPage("host1", "ns:nonPage", "Bob"));
    }
}