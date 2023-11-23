package us.calubrecht.lazerwiki.model;

public record PageData(String rendered, String source, boolean exists, boolean userCanRead, boolean userCanWrite) {
}
