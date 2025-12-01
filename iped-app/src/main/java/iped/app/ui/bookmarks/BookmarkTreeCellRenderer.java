package iped.app.ui.bookmarks;

import java.awt.Color;
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
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
    private final boolean bookmarkTabTree;

    public BookmarkTreeCellRenderer() {
        this(true);
    }

    public BookmarkTreeCellRenderer(boolean bookmarkTabTree) {
        this.bookmarkTabTree = bookmarkTabTree;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
            int row, boolean hasFocus) {

        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        if (value.equals(BookmarksTreeModel.ROOT)) {
            setIcon(rootIcon);
        }
        IMultiBookmarks bookmarks = App.get().appCase.getMultiBookmarks();
        if (bookmarks != null) {
            // collect full bookmark path(s) represented by this tree node
            Set<String> fullPaths = ((BookmarksTreeModel) tree.getModel()).collectAllFullPaths(value);
            if (value.equals(BookmarksTreeModel.NO_BOOKMARKS)) {
                setToolTipText(null);
                setIcon(BookmarkIcon.getIcon(BookmarkStandardColors.noBookmarksColor));
            } else if (!value.equals(BookmarksTreeModel.ROOT)) {
                if (fullPaths.size() == 1) {
                    String name = fullPaths.iterator().next();
                    String nodeFullPath = resolveFullPathForNode(tree, value, row);
                    boolean nodeIsBookmark = nodeFullPath != null && nodeFullPath.equals(name) && bookmarks.getBookmarkSet().contains(nodeFullPath);

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
                        String leafName = getLeafNameFromPath(name);
                        if (bookmarkTabTree && count > 0) {
                            setText(leafName + " (" + LocalizedFormat.format(count) + ")");
                        } else {
                            setText(leafName);
                        }
                    }
                    else {
                        setToolTipText(null);
                        Color childColor = bookmarks.getBookmarkColor(name);
                        setIcon(BookmarkIcon.getIcon(childColor == null ? BookmarkStandardColors.defaultColor : childColor));
                        int count = bookmarks.getBookmarkCount(name);
                        if (bookmarkTabTree && count > 0) {
                            setText(value.toString() + " (" + LocalizedFormat.format(count) + ")");
                        }
                    }
                }
                else if (fullPaths.size() > 1) {
                    String nodeFullPath = resolveFullPathForNode(tree, value, row);
                    String name = value == null ? null : value.toString();
                    if (nodeFullPath == null) {
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
                        }
                        else {
                            setToolTipText(null);
                        }
                        Color color = bookmarks.getBookmarkColor(nodeFullPath);
                        setIcon(BookmarkIcon.getIcon(color == null ? BookmarkStandardColors.defaultColor : color));
                        int total = 0;
                        for (String p : fullPaths)
                            total += bookmarks.getBookmarkCount(p);
                        if (bookmarkTabTree && total > 0) {
                            String leafName = getLeafNameFromPath(nodeFullPath);
                            setText(leafName + " (" + LocalizedFormat.format(total) + ")");
                        }
                    }
                    else {
                        setToolTipText(null);
                        setIcon(BookmarkIcon.getIcon(BookmarkStandardColors.defaultColor));
                        int total = 0;
                        for (String p : fullPaths)
                            total += bookmarks.getBookmarkCount(p);
                        if (bookmarkTabTree && total > 0)
                            setText(value.toString() + " (" + LocalizedFormat.format(total) + ")");
                    }
                }
            }
        }
        return this;
    }

    private String resolveFullPathForNode(JTree tree, Object value, int row) {
        TreePath tp = null;
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

    private TreePath findPathForValue(JTree tree, Object value) {
        TreeModel model = tree.getModel();
        TreePath rootPath = new TreePath(model.getRoot());
        return findPath(rootPath, model, value);
    }

    private TreePath findPath(TreePath path, TreeModel model, Object value) {
        if (path.getLastPathComponent().equals(value))
            return path;
        Object node = path.getLastPathComponent();
        int cc = model.getChildCount(node);
        for (int i = 0; i < cc; i++) {
            Object child = model.getChild(node, i);
            TreePath childPath = path.pathByAddingChild(child);
            TreePath found = findPath(childPath, model, value);
            if (found != null)
                return found;
        }
        return null;
    }

    private static String getLeafNameFromPath(String fullPath) {
        if (fullPath == null)
            return null;
        int i = fullPath.lastIndexOf(BookmarksTreeModel.SEPARATOR);
        return i < 0 ? fullPath : fullPath.substring(i + 1);
    }

}
