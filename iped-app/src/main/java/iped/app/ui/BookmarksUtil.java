package iped.app.ui;

import javax.swing.tree.TreePath;

public final class BookmarksUtil {

    private BookmarksUtil() {}

    /**
     * Compute the full bookmark name given user input and a selected tree path.
     * - if a single node is selected, build a child full path by joining the selected
     *   node path segments (excluding ROOT and the NO_BOOKMARKS sentinel) and the input
    * - otherwise return the input as a top-level (root) bookmark name
    *
    * Slashes in the input are NOT interpreted as parent/child separators. Any
    * '/' characters typed by the user are escaped as "%2F" so they won't
    * create extra levels in the stored bookmark path. The selected tree node
    * (if any) is always used as the parent when creating a child bookmark.
    */
    public static String computeNewBookmarkFullName(String input, TreePath selectedSinglePath) {
        // sanitize input so literal separator sequence doesn't create hierarchy:
        // - first escape any literal '/' typed by user so it won't be interpreted
        //   as the old separator (preserve previous behavior)
        // - then escape the configured SEPARATOR (eg. ">>>" -> "%3E%3E%3E")
        if (input == null)
            input = "";
        String sanitized = input.replace("/", "%2F");
        sanitized = sanitized.replace(BookmarksTreeModel.SEPARATOR, "%3E%3E%3E");

        String fullName = sanitized;
        if (selectedSinglePath != null) {
            Object[] parts = selectedSinglePath.getPath();
            if (parts.length > 1) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < parts.length; i++) {
                    Object seg = parts[i];
                    // ignore NO_BOOKMARKS sentinel: treat selecting it as root
                    if (seg == BookmarksTreeModel.NO_BOOKMARKS)
                        continue;
                    String part = seg.toString();
                    if (sb.length() > 0)
                        sb.append(BookmarksTreeModel.SEPARATOR);
                    sb.append(part);
                }
                if (sb.length() > 0)
                    fullName = sb.toString() + BookmarksTreeModel.SEPARATOR + sanitized;
            }
        }
        return fullName;
    }

    /**
     * Given a list of bookmark full paths (eg. "Parent/Child"), return a
     * single string suited for UI display composed of only the leaf names
     * joined with " | ". Null-safe.
     */
    public static String concatLeafNames(java.util.List<String> fullPaths) {
        if (fullPaths == null)
            return null;
        if (fullPaths.isEmpty())
            return "";
        java.util.List<String> leafs = new java.util.ArrayList<>(fullPaths.size());
        for (String s : fullPaths) {
            if (s == null) {
                leafs.add("");
            } else {
                int i = s.lastIndexOf(BookmarksTreeModel.SEPARATOR);
                leafs.add(i < 0 ? s : s.substring(i + BookmarksTreeModel.SEPARATOR.length()));
            }
        }
        return iped.engine.util.Util.concatStrings(leafs);
    }

}
