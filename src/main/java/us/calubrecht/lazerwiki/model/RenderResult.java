package us.calubrecht.lazerwiki.model;

import java.util.List;

public record RenderResult (String renderedText, String title, List<PageDescriptor> internalLinks) {
}
