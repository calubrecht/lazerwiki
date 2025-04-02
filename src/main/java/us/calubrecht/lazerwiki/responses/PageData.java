package us.calubrecht.lazerwiki.responses;

import us.calubrecht.lazerwiki.model.PerfTracker;

import java.util.List;
import java.util.Map;

public record PageData(String rendered, String source, String title, List<String> tags, List<String> backlinks, PageFlags flags, Long id, Long revision, boolean success, String msg, PerfTracker perfTracker) {

    public PageData(String rendered, String source, String title, List<String> tags, List<String> backlinks, PageFlags flags, Long id, Long revision, boolean success, String msg) {
       this(rendered, source, title, tags, backlinks, flags, id, revision, success, msg, null);
  ;  }

    public PageData(String rendered, String source, String title, List<String> tags, List<String> backlinks, PageFlags flags, Long id, Long revision) {
        this(rendered, source, title, tags, backlinks, flags, id, revision, true, null);
    }

    public PageData(String rendered, String source, List<String> tags, List<String> backlinks, PageFlags flags) {
        this(rendered, source, null, tags, backlinks, flags, null, null, true, null);
    }

    public PageData(String rendered, String source, String title, List<String> tags, List<String> backlinks, PageFlags flags) {
        this(rendered, source, title, tags, backlinks, flags, null, null, true, null);
    }
    public record PageFlags(boolean exists, boolean wasDeleted, boolean userCanRead, boolean userCanWrite, boolean userCanDelete, boolean moved) {

        public Map<String, Boolean> toMap() {
            return Map.of("exists", exists, "wasDeleted", wasDeleted, "userCanRead", userCanRead, "userCanWrite", userCanWrite, "userCanDelete", userCanDelete);
        }

    }

    public PageData(String rendered, String source, String title, List<String> tags, List<String> backlinks, PageFlags flags, Long id) {
        this(rendered, source, title, tags, backlinks, flags, id, null, true, null);
    }

    public static final PageFlags EMPTY_FLAGS = new PageFlags(false, false, false, false, false, false);
    public static final PageFlags ALL_RIGHTS = new PageFlags(true, false, true, true, true, false);
}
