package iped.app.ui.bookmarks;

import java.awt.Color;
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import iped.app.ui.App;
import iped.app.ui.BookmarksTreeModel;
import iped.app.ui.IconManager;
import iped.data.IMultiBookmarks;
import iped.utils.LocalizedFormat;
import java.util.Set;

public class BookmarkTreeCellRenderer extends DefaultTreeCellRenderer {
    private static final long serialVersionUID = 1L;

    private static final Icon rootIcon = IconManager.getTreeIcon("bookmarks-root");
    private final IMultiBookmarks injectedBookmarks;
    private final boolean showCounts;

    public BookmarkTreeCellRenderer() {
        this(null, true);
    }

    // constructor used for tests to inject a stub IMultiBookmarks
    public BookmarkTreeCellRenderer(IMultiBookmarks injectedBookmarks) {
        this(injectedBookmarks, true);
    }

    // Full constructor (used by dialog/tab to control visual details)
    public BookmarkTreeCellRenderer(IMultiBookmarks injectedBookmarks, boolean showCounts) {
        this.injectedBookmarks = injectedBookmarks;
        this.showCounts = showCounts;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
            int row, boolean hasFocus) {

        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        if (value.equals(BookmarksTreeModel.ROOT)) {
            setIcon(rootIcon);
        }
        IMultiBookmarks bookmarks = this.injectedBookmarks != null ? this.injectedBookmarks
            : (App.get().appCase != null ? App.get().appCase.getMultiBookmarks() : null);
        if (bookmarks != null) {
            // collect full bookmark path(s) represented by this tree node
            Set<String> fullPaths = ((BookmarksTreeModel) tree.getModel()).collectAllFullPaths(value);
            if (value.equals(BookmarksTreeModel.NO_BOOKMARKS)) {
                setToolTipText(null);
                setIcon(BookmarkIcon.getIcon(BookmarkStandardColors.noBookmarksColor));
            } else if (!value.equals(BookmarksTreeModel.ROOT)) {
                if (fullPaths.size() == 1) {
                    String name = fullPaths.iterator().next();

                    // If the node is the actual bookmark (node full path equals
                    // the single collected full path), show its color. Otherwise
                    // the node is a folder containing that single child bookmark
                    // and we should NOT use the child's color for the parent —
                    // prefer the default single-color icon instead.
                    String nodeFullPath = resolveFullPathForNode(tree, value, row);
                    boolean nodeIsBookmark = nodeFullPath != null && nodeFullPath.equals(name)
                            && bookmarks.getBookmarkSet().contains(nodeFullPath);

                    if (nodeIsBookmark) {
                        String comment = bookmarks.getBookmarkComment(name);
                        if (comment != null && !comment.trim().isEmpty()) {
                            setToolTipText(comment.trim());
                        } else {
                            setToolTipText(null);
                        }
                        Color color = bookmarks.getBookmarkColor(name);
                        setIcon(BookmarkIcon.getIcon(color == null ? BookmarkStandardColors.defaultColor : color));
                        int count = bookmarks.getBookmarkCount(name);
                        // show the full path for bookmark nodes to avoid truncated display
                        String display = lastSegment(name);
                        if (showCounts && count > 0) {
                            setText(display + " (" + LocalizedFormat.format(count) + ")");
                        } else {
                            setText(display);
                        }
                    } else {
                        // parent is not the bookmark itself — use the first
                        // child bookmark's color as the parent's initial color
                        // (single-color), instead of a merged/multi-color icon.
                        setToolTipText(null);
                        Color childColor = bookmarks.getBookmarkColor(name);
                        setIcon(BookmarkIcon.getIcon(childColor == null ? BookmarkStandardColors.defaultColor : childColor));
                        int count = bookmarks.getBookmarkCount(name);
                        if (showCounts && count > 0) {
                            setText(value.toString() + " (" + LocalizedFormat.format(count) + ")");
                        }
                    }
                } else if (fullPaths.size() > 1) {
                    // If this tree node represents its own bookmark as well as
                    // a folder (eg. "Parent" and "Parent/Child"), prefer the
                    // node's own color so the parent keeps the color assigned
                    // to its own bookmark at creation-time. Otherwise fall back
                    // to the aggregated (joined) icon for the group.
                    // Try row-based resolution first (reliable in the running UI).
                    String nodeFullPath = resolveFullPathForNode(tree, value, row);
                    // node display name (last segment)
                        String name = value == null ? null : value.toString();
                        if (nodeFullPath == null) {
                        // fallback heuristic: pick a fullPath whose last segment
                        // equals the node's display name; choose the shortest
                        // matching path to prefer the node itself rather than a
                        // descendant with the same segment name.
                        if (name != null) {
                            int bestLen = Integer.MAX_VALUE;
                            for (String s : fullPaths) {
                                if (s.equals(name) || s.endsWith(BookmarksTreeModel.SEPARATOR + name)) {
                                    int len = s.split(java.util.regex.Pattern.quote(BookmarksTreeModel.SEPARATOR)).length;
                                    if (len < bestLen) {
                                        bestLen = len;
                                        nodeFullPath = s;
                                    }
                                }
                            }
                        }
                    }
                        if (nodeFullPath != null && bookmarks.getBookmarkSet().contains(nodeFullPath)) {
                        String comment = bookmarks.getBookmarkComment(nodeFullPath);
                        if (comment != null && !comment.trim().isEmpty()) {
                            setToolTipText(comment.trim());
                        } else {
                            setToolTipText(null);
                        }
                        Color color = bookmarks.getBookmarkColor(nodeFullPath);
                        setIcon(BookmarkIcon.getIcon(color == null ? BookmarkStandardColors.defaultColor : color));
                        // sum counts (still useful to show combined count)
                        int total = 0;
                        for (String p : fullPaths)
                            total += bookmarks.getBookmarkCount(p);
                        if (showCounts && total > 0) {
                            String display = lastSegment(nodeFullPath);
                            setText(display + " (" + LocalizedFormat.format(total) + ")");
                        }
                    } else {
                        // Do NOT show a merged/multi-color icon for parent nodes.
                        // If the node itself is not a bookmark but multiple child
                        // bookmarks exist, prefer the first child's color as the
                        // parent's initial single-color icon (keeps parent visuals
                        // stable but gives a reasonable color based on content)
                        setToolTipText(null);
                        // pick the first fullPath from the sorted set as the
                        // initial child's color (deterministic ordering)
                        String firstChildPath = fullPaths.iterator().next();
                        if (firstChildPath != null && bookmarks.getBookmarkSet().contains(firstChildPath)) {
                            Color color = bookmarks.getBookmarkColor(firstChildPath);
                            setIcon(BookmarkIcon.getIcon(color == null ? BookmarkStandardColors.defaultColor : color));
                        } else {
                            setIcon(BookmarkIcon.getIcon(BookmarkStandardColors.defaultColor));
                        }
                        // sum counts
                        int total = 0;
                        for (String p : fullPaths)
                            total += bookmarks.getBookmarkCount(p);
                        if (showCounts && total > 0)
                            setText(value.toString() + " (" + LocalizedFormat.format(total) + ")");
                    }
                }
            }
            }
        return this;
    }

    private String resolveFullPathForNode(JTree tree, Object value, int row) {
        javax.swing.tree.TreePath tp = null;
        if (row >= 0) {
            tp = tree.getPathForRow(row);
        }
        if (tp == null) {
            tp = findPathForValue(tree, value);
        }
        if (tp == null)
            return null;
        Object[] parts = tp.getPath();
        if (parts.length <= 1)
            return null;
        StringBuilder sb = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
            if (sb.length() > 0)
                sb.append(BookmarksTreeModel.SEPARATOR);
            sb.append(parts[i].toString());
        }
        return sb.toString();
    }

    private javax.swing.tree.TreePath findPathForValue(JTree tree, Object value) {
        javax.swing.tree.TreeModel model = tree.getModel();
        javax.swing.tree.TreePath rootPath = new javax.swing.tree.TreePath(model.getRoot());
        return findPathRec(rootPath, model, value);
    }

    private javax.swing.tree.TreePath findPathRec(javax.swing.tree.TreePath path, javax.swing.tree.TreeModel model, Object value) {
        if (path.getLastPathComponent().equals(value))
            return path;
        Object node = path.getLastPathComponent();
        int cc = model.getChildCount(node);
        for (int i = 0; i < cc; i++) {
            Object child = model.getChild(node, i);
            javax.swing.tree.TreePath childPath = path.pathByAddingChild(child);
            javax.swing.tree.TreePath found = findPathRec(childPath, model, value);
            if (found != null)
                return found;
        }
        return null;
    }

    // Return the last path segment (leaf name) from a full bookmark path
    // e.g. "Parent/Child" -> "Child"; null-safe
    private static String lastSegment(String fullPath) {
        if (fullPath == null)
            return null;
        int i = fullPath.lastIndexOf(BookmarksTreeModel.SEPARATOR);
        return i < 0 ? fullPath : fullPath.substring(i + 1);
    }

    

    
}
