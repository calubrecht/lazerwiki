package us.calubrecht.lazerwiki.service;

import us.calubrecht.lazerwiki.model.RenderResult;

/**
 * Interface for a renderer that converts a source in a specific markup language to an HTML snippet for display.
 * A markup renderer should be responsible for sanitizing user-input, prevent the rendering of scripts or other
 * dangerous content.
 */
public interface IMarkupRenderer {

    public RenderResult renderWithInfo(String markup, String host, String site);

    public default String  renderToString(String markup, String host, String site) {
        return renderWithInfo(markup, host, site).renderedText();
    }

}
