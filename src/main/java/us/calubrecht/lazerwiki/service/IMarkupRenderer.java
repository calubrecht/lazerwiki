package us.calubrecht.lazerwiki.service;

/**
 * Interface for a renderer that converts a source in a specific markup language to an HTML snippet for display.
 * A markup renderer should be responsible for sanitizing user-input, prevent the rendering of scripts or other
 * dangerous content.
 */
public interface IMarkupRenderer {

    public String render(String markup, String host, String site);
}
