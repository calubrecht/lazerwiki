package us.calubrecht.lazerwiki.syntax.framework;

public interface ReadOnlyParseContext {
  String getFullText();

  ReadOnlyParseContext getRootContext();
}
