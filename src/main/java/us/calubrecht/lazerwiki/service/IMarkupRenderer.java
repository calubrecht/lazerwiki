package us.calubrecht.lazerwiki.service;

import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

/**
 * Interface for a renderer that converts a source in a specific markup language to an HTML snippet for display.
 * A markup renderer should be responsible for sanitizing user-input, prevent the rendering of scripts or other
 * dangerous content.
 */
public interface IMarkupRenderer {

    RenderResult renderWithInfo(String markup, RenderContext renderContext);

    String  renderToString(String markup, RenderContext renderContext);

    String renderToPlainText(String markup, RenderContext renderContext);

    default String  renderToString(String markup, String host, String site, String user) {
        return renderToString(markup, new RenderContext(host, site, user));
    }

    default RenderResult  renderWithInfo(String markup, String host, String site, String user) {
        return renderWithInfo(markup, new RenderContext(host, site, user));
    }


}
