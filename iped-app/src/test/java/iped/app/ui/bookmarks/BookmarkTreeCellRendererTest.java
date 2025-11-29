package iped.app.ui.bookmarks;

import static org.junit.Assert.*;

import java.awt.Color;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JTree;
import javax.swing.tree.TreePath;

import org.junit.Test;

import iped.app.ui.BookmarksTreeModel;
import iped.data.IMultiBookmarks;

public class BookmarkTreeCellRendererTest {

    static class StubMultiBookmarks implements IMultiBookmarks {
        Set<String> set = new TreeSet<>();

        @Override
        public Set<String> getBookmarkSet() {
            return set;
        }

        @Override
        public String getBookmarkComment(String bookmarkName) {
            return null;
        }

        @Override
        public Color getBookmarkColor(String bookmarkName) {
            if ("Parent".equals(bookmarkName))
                return Color.RED;
            if ("Parent>>>Child1".equals(bookmarkName))
                return Color.BLUE;
            if ("Parent>>>Child2".equals(bookmarkName))
                return Color.GREEN;
            return null;
        }

        @Override
        public int getBookmarkCount(String bookmarkName) {
            return 0;
        }

        // stub remaining methods with no-op/defaults
        @Override public void addBookmark(java.util.Set ids, String bookmarkName) {}
        @Override public void addToTypedWords(String texto) {}
        @Override public void renameBookmark(String oldBookmark, String newBookmark) {}
        @Override public void clearChecked() {}
        @Override public void clearTypedWords() {}
        @Override public void delBookmark(String bookmarkName) {}
        @Override public iped.search.IMultiSearchResult filterBookmarks(iped.search.IMultiSearchResult result, java.util.Set<String> bookmarkNames) { return null; }
        @Override public iped.search.IMultiSearchResult filterChecked(iped.search.IMultiSearchResult result) { return null; }
        @Override public iped.search.IMultiSearchResult filterBookmarksOrNoBookmarks(iped.search.IMultiSearchResult result, java.util.Set<String> bookmarkNames) { return null; }
        @Override public iped.search.IMultiSearchResult filterNoBookmarks(iped.search.IMultiSearchResult result) { return null; }
        @Override public java.util.List<String> getBookmarkList(iped.data.IItemId item) { return null; }
        @Override public java.util.Collection<iped.data.IBookmarks> getSingleBookmarks() { return null; }
        @Override public int getTotalChecked() { return 0; }
        @Override public java.util.LinkedHashSet<String> getTypedWords() { return null; }
        @Override public boolean hasBookmark(iped.data.IItemId item) { return false; }
        @Override public boolean hasBookmark(iped.data.IItemId item, java.util.Set<String> bookmarkNames) { return false; }
        @Override public boolean hasBookmark(iped.data.IItemId item, String bookmarkName) { return false; }
        @Override public boolean isChecked(iped.data.IItemId item) { return false; }
        @Override public void loadState() {}
        @Override public void loadState(java.io.File file) throws ClassNotFoundException, java.io.IOException {}
        @Override public void newBookmark(String bookmarkName) { set.add(bookmarkName); }
        @Override public void removeBookmark(java.util.Set ids, String bookmarkName) {}
        @Override public void saveState() {}
        @Override public void saveState(boolean sync) {}
        @Override public void saveState(java.io.File file) throws java.io.IOException {}
        @Override public void setBookmarkKeyStroke(String bookmarkName, javax.swing.KeyStroke key) {}
        @Override public javax.swing.KeyStroke getBookmarkKeyStroke(String bookmarkName) { return null; }
        @Override public void removeBookmarkKeyStroke(String bookmarkName) {}
        @Override public void checkAll() {}
        @Override public void setChecked(boolean value, iped.data.IItemId item) {}
        @Override public boolean isInReport(String bookmark) { return false; }
        @Override public void setInReport(String bookmark, boolean checked) {}
        @Override public void addSelectionListener(iped.data.SelectionListener listener) {}
        @Override public void removeSelectionListener(iped.data.SelectionListener listener) {}
        @Override public void addBookmark(java.util.List ids, String bookmarkName) {}
        @Override public void removeBookmark(java.util.List ids, String bookmarkName) {}
        @Override public void setBookmarkColor(String bookmarkName, Color color) {}
        @Override public void setBookmarkComment(String texto, String comment) {}
        @Override public java.util.Set<Color> getUsedColors() { return null; }
        

    }

    @Test
    public void parentKeepsItsOwnColor_whenParentAlsoExists() {
        BookmarksTreeModel model = new BookmarksTreeModel();
        model.bookmarks = new TreeSet<>(Arrays.asList("Parent", "Parent>>>Child1"));

        JTree tree = new JTree(model);

        Object parentNode = model.getChild(BookmarksTreeModel.ROOT, 1);

        StubMultiBookmarks stub = new StubMultiBookmarks();
        stub.set.add("Parent");
        stub.set.add("Parent>>>Child1");

        BookmarkTreeCellRenderer renderer = new BookmarkTreeCellRenderer(stub);

        // note: model.getNodeForFullPath may build a fresh internal tree and
        // return a node instance that is not object-equal to the instance
        // previously returned by getChild(), so we don't rely on identity here.

        // try to expand and find a row for the parent so resolveFullPathForNode
        // will detect the parent full path. If rows are not available, the
        // renderer will also try the model traversal fallback.
        tree.expandPath(new TreePath(new Object[] { BookmarksTreeModel.ROOT }));
        TreePath parentPath = new TreePath(new Object[] { BookmarksTreeModel.ROOT, parentNode });
        tree.expandPath(parentPath);
        int row = tree.getRowForPath(parentPath);
        if (row < 0) {
            // fallback: scan the rows for the path whose last component equals parentNode
            for (int i = 0; i < tree.getRowCount(); i++) {
                TreePath p = tree.getPathForRow(i);
                if (p != null && p.getLastPathComponent().equals(parentNode)) {
                    row = i;
                    break;
                }
            }
        }

        renderer.getTreeCellRendererComponent(tree, parentNode, false, false, false, row, false);

        assertTrue(renderer.getIcon() instanceof BookmarkIcon);
        BookmarkIcon icon = (BookmarkIcon) renderer.getIcon();
        assertEquals(Color.RED, icon.getColor());
    }

    @Test
    public void parentDoesNotShowMergedColors_whenParentIsNotABookmark() {
        BookmarksTreeModel model = new BookmarksTreeModel();
        model.bookmarks = new TreeSet<>(Arrays.asList("Parent>>>Child1"));

        JTree tree = new JTree(model);

        Object parentNode = model.getChild(BookmarksTreeModel.ROOT, 1);

        StubMultiBookmarks stub = new StubMultiBookmarks();
        stub.set.add("Parent>>>Child1");

        BookmarkTreeCellRenderer renderer = new BookmarkTreeCellRenderer(stub);

        tree.expandPath(new TreePath(new Object[] { BookmarksTreeModel.ROOT }));
        TreePath parentPath = new TreePath(new Object[] { BookmarksTreeModel.ROOT, parentNode });
        tree.expandPath(parentPath);
        int row = tree.getRowForPath(parentPath);
        if (row < 0) {
            for (int i = 0; i < tree.getRowCount(); i++) {
                TreePath p = tree.getPathForRow(i);
                if (p != null && p.getLastPathComponent().equals(parentNode)) {
                    row = i;
                    break;
                }
            }
        }

        renderer.getTreeCellRendererComponent(tree, parentNode, false, false, false, row, false);

        assertTrue(renderer.getIcon() instanceof BookmarkIcon);
        BookmarkIcon icon = (BookmarkIcon) renderer.getIcon();
        // When parent itself is NOT a bookmark, we should render a single
        // color icon matching the first child's color (initial parent color)
        assertEquals(Color.BLUE, icon.getColor());
    }

    @Test
    public void parentUsesFirstChildColor_whenParentNotBookmark_withMultipleChildren() {
        BookmarksTreeModel model = new BookmarksTreeModel();
        model.bookmarks = new TreeSet<>(Arrays.asList("Parent>>>Child1", "Parent>>>Child2"));

        JTree tree = new JTree(model);

        Object parentNode = model.getChild(BookmarksTreeModel.ROOT, 1);

        StubMultiBookmarks stub = new StubMultiBookmarks();
        stub.set.add("Parent>>>Child1");
        stub.set.add("Parent>>>Child2");

        BookmarkTreeCellRenderer renderer = new BookmarkTreeCellRenderer(stub);

        tree.expandPath(new TreePath(new Object[] { BookmarksTreeModel.ROOT }));
        TreePath parentPath = new TreePath(new Object[] { BookmarksTreeModel.ROOT, parentNode });
        tree.expandPath(parentPath);
        int row = tree.getRowForPath(parentPath);
        if (row < 0) {
            for (int i = 0; i < tree.getRowCount(); i++) {
                TreePath p = tree.getPathForRow(i);
                if (p != null && p.getLastPathComponent().equals(parentNode)) {
                    row = i;
                    break;
                }
            }
        }

        renderer.getTreeCellRendererComponent(tree, parentNode, false, false, false, row, false);

        assertTrue(renderer.getIcon() instanceof BookmarkIcon);
        BookmarkIcon icon = (BookmarkIcon) renderer.getIcon();
        // Parent should take first child's color (Child1 -> BLUE)
        assertEquals(Color.BLUE, icon.getColor());
    }

    @Test
    public void childBookmarkShowsOnlyLeafName() {
        BookmarksTreeModel model = new BookmarksTreeModel();
        model.bookmarks = new TreeSet<>(Arrays.asList("Parent>>>Child1"));

        JTree tree = new JTree(model);

        Object childNode = model.getNodeForFullPath("Parent>>>Child1");

        StubMultiBookmarks stub = new StubMultiBookmarks();
        stub.set.add("Parent>>>Child1");

        BookmarkTreeCellRenderer renderer = new BookmarkTreeCellRenderer(stub);

        renderer.getTreeCellRendererComponent(tree, childNode, false, false, true, -1, false);

        assertEquals("Child1", renderer.getText());
    }

    @Test
    public void parentNodeShowsOnlyName_whenMultipleChildren() {
        BookmarksTreeModel model = new BookmarksTreeModel();
        model.bookmarks = new TreeSet<>(Arrays.asList("Parent>>>Child1", "Parent>>>Child2"));

        JTree tree = new JTree(model);

        Object parentNode = model.getChild(BookmarksTreeModel.ROOT, 1);

        StubMultiBookmarks stub = new StubMultiBookmarks();
        stub.set.add("Parent>>>Child1");
        stub.set.add("Parent>>>Child2");

        BookmarkTreeCellRenderer renderer = new BookmarkTreeCellRenderer(stub);

        renderer.getTreeCellRendererComponent(tree, parentNode, false, false, false, -1, false);

        assertEquals("Parent", renderer.getText());
    }

}
