package us.calubrecht.lazerwiki.responses;

import java.util.List;

public record PageData(String rendered, String source, List<String> tags, List<String> backlinks, PageFlags flags) {
    public record PageFlags(boolean exists, boolean wasDeleted, boolean userCanRead, boolean userCanWrite, boolean userCanDelete) {

    }

    public static final PageFlags EMPTY_FLAGS = new PageFlags(false, false, false, false, false);
    public static final PageFlags ALL_RIGHTS = new PageFlags(true, false, true, true, true);
}
