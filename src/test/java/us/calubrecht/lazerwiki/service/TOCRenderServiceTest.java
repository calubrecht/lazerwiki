package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import us.calubrecht.lazerwiki.model.HeaderRef;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TOCRenderServiceTest {
    TOCRenderService service = new TOCRenderService();

    @Test
    void renderTOCSimple() {
        List<HeaderRef> headers = List.of(
                new HeaderRef(1, "Header 1", "Header_1"),
                new HeaderRef(1, "Header 2", "Header_2"),
                new HeaderRef(1, "Header 3", "Header_3")
        );
        String headerRender = """
                <div class="TOC" id="lw_TOC">
                  <ol>
                    <li><a href="#Header_1">Header 1</a></li>
                    <li><a href="#Header_2">Header 2</a></li>
                    <li><a href="#Header_3">Header 3</a></li>
                  </ol>
                </div>""";
        assertEquals(headerRender, service.renderTOC(headers, ""));
    }

    @Test
    void renderTOCLeveld() {
        List<HeaderRef> headers = List.of(
                new HeaderRef(1, "Header 1", "Header_1"),
                new HeaderRef(3, "Header 2", "Header_2"),
                new HeaderRef(1, "Header 3", "Header_3"),
                new HeaderRef(2, "Header 2", "Header_2_1")
        );
        String headerRender = """
                <div class="TOC" id="lw_TOC">
                  <ol>
                    <li><a href="#Header_1">Header 1</a></li>
                    <ol>
                      <li><a href="#Header_2">Header 2</a></li>
                    </ol>
                    <li><a href="#Header_3">Header 3</a></li>
                    <ol>
                      <li><a href="#Header_2_1">Header 2</a></li>
                    </ol>
                  </ol>
                </div>""";
        assertEquals(headerRender, service.renderTOC(headers, ""));
    }

    @Test
    void renderTOCDown2levls() {
        List<HeaderRef> headers = List.of(
                new HeaderRef(1, "Header 1", "Header_1"),
                new HeaderRef(2, "Header 2", "Header_2"),
                new HeaderRef(3, "Header 3", "Header_3"),
                new HeaderRef(1, "Header 2", "Header_2_1")
        );
        String headerRender = """
                <div class="TOC" id="lw_TOC">
                  <ol>
                    <li><a href="#Header_1">Header 1</a></li>
                    <ol>
                      <li><a href="#Header_2">Header 2</a></li>
                      <ol>
                        <li><a href="#Header_3">Header 3</a></li>
                      </ol>
                    </ol>
                    <li><a href="#Header_2_1">Header 2</a></li>
                  </ol>
                </div>""";
        assertEquals(headerRender, service.renderTOC(headers, ""));
    }

    @Test
    void renderTOCstartLow() {
        List<HeaderRef> headers = List.of(
                new HeaderRef(3, "Header 1", "Header_1"),
                new HeaderRef(1, "Header 2", "Header_2"),
                new HeaderRef(2, "Header 3", "Header_3")
        );
        String headerRender = """
                <div class="TOC" id="lw_TOC">
                  <ol>
                    <li><a href="#Header_1">Header 1</a></li>
                    <li><a href="#Header_2">Header 2</a></li>
                    <ol>
                      <li><a href="#Header_3">Header 3</a></li>
                    </ol>
                  </ol>
                </div>""";
        assertEquals(headerRender, service.renderTOC(headers, ""));
    }

    @Test
    void renderTOCSimpleWSuffix() {
        List<HeaderRef> headers = List.of(
                new HeaderRef(1, "Header 1", "Header_1"),
                new HeaderRef(1, "Header 2", "Header_2"),
                new HeaderRef(1, "Header 3", "Header_3")
        );
        String headerRender = """
                <div class="TOC" id="lw_TOC_wSuffix">
                  <ol>
                    <li><a href="#Header_1_wSuffix">Header 1</a></li>
                    <li><a href="#Header_2_wSuffix">Header 2</a></li>
                    <li><a href="#Header_3_wSuffix">Header 3</a></li>
                  </ol>
                </div>""";
        assertEquals(headerRender, service.renderTOC(headers, "_wSuffix"));
    }
}