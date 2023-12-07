package us.calubrecht.lazerwiki.responses;

import java.util.List;
import java.util.Map;

public record PageData(String rendered, String source, String title, List<String> tags, List<String> backlinks, PageFlags flags) {

    public PageData(String rendered, String source, List<String> tags, List<String> backlinks, PageFlags flags) {
        this(rendered, source, null, tags, backlinks, flags);
    }
    public record PageFlags(boolean exists, boolean wasDeleted, boolean userCanRead, boolean userCanWrite, boolean userCanDelete) {

        public Map<String, Boolean> toMap() {
            return Map.of("exists", exists, "wasDeleted", wasDeleted, "userCanRead", userCanRead, "userCanWrite", userCanWrite, "userCanDelete", userCanDelete);
        }

    }

    public static final PageFlags EMPTY_FLAGS = new PageFlags(false, false, false, false, false);
    public static final PageFlags ALL_RIGHTS = new PageFlags(true, false, true, true, true);
}
