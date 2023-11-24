package us.calubrecht.lazerwiki.responses;

import java.util.List;

public record PageData(String rendered, String source, List<String> tags, boolean exists, boolean userCanRead, boolean userCanWrite) {
}