package us.calubrecht.lazerwiki.responses;

import java.util.List;

public record PageData(String rendered, String source, List<String> tags, List<String> backlinks, boolean exists, boolean userCanRead, boolean userCanWrite) {
}
