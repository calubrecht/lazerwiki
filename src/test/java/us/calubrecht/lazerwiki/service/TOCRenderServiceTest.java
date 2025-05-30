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
                <div id="lw_TOC">
                  <ol>
                    <li>Header 1</li>
                    <li>Header 2</li>
                    <li>Header 3</li>
                  </ol>
                </div>""";
        assertEquals(headerRender, service.renderTOC(headers));
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
                <div id="lw_TOC">
                  <ol>
                    <li>Header 1</li>
                    <ol>
                      <li>Header 2</li>
                    </ol>
                    <li>Header 3</li>
                    <ol>
                      <li>Header 2</li>
                    </ol>
                  </ol>
                </div>""";
        assertEquals(headerRender, service.renderTOC(headers));
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
                <div id="lw_TOC">
                  <ol>
                    <li>Header 1</li>
                    <ol>
                      <li>Header 2</li>
                      <ol>
                        <li>Header 3</li>
                      </ol>
                    </ol>
                    <li>Header 2</li>
                  </ol>
                </div>""";
        assertEquals(headerRender, service.renderTOC(headers));
    }

    @Test
    void renderTOCstartLow() {
        List<HeaderRef> headers = List.of(
                new HeaderRef(3, "Header 1", "Header_1"),
                new HeaderRef(1, "Header 2", "Header_2"),
                new HeaderRef(2, "Header 3", "Header_3")
        );
        String headerRender = """
                <div id="lw_TOC">
                  <ol>
                    <li>Header 1</li>
                    <li>Header 2</li>
                    <ol>
                      <li>Header 3</li>
                    </ol>
                  </ol>
                </div>""";
        assertEquals(headerRender, service.renderTOC(headers));
    }
}