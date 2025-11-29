package iped.app.ui;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.TreeSet;

import javax.swing.tree.TreePath;

import org.junit.Test;

public class BookmarksManagerNewTest {

    @Test
    public void newBookmarkAtRoot_whenRootSelected() {
        BookmarksTreeModel model = new BookmarksTreeModel();
        // no bookmarks in model -> NO_BOOKMARKS sentinel will be present
        model.bookmarks = new TreeSet<>();

        TreePath rootPath = new TreePath(new Object[] { BookmarksTreeModel.ROOT });

        String full = BookmarksUtil.computeNewBookmarkFullName("NewTop", rootPath);
        assertEquals("NewTop", full);
    }

    @Test
    public void newBookmarkWithSlash_whenRootSelected() {
        BookmarksTreeModel model = new BookmarksTreeModel();
        model.bookmarks = new TreeSet<>();

        TreePath rootPath = new TreePath(new Object[] { BookmarksTreeModel.ROOT });

        String full = BookmarksUtil.computeNewBookmarkFullName("A/B", rootPath);
        assertEquals("A%2FB", full);
    }

    @Test
    public void newBookmarkAtRoot_whenNoBookmarksSelected() {
        BookmarksTreeModel model = new BookmarksTreeModel();
        model.bookmarks = new TreeSet<>();

        // no tree instance needed - root + NO_BOOKMARKS sentinel are represented
        // directly as constants in the TreePath used below
        // NO_BOOKMARKS is the first child of root
        Object no = BookmarksTreeModel.NO_BOOKMARKS;
        TreePath path = new TreePath(new Object[] { BookmarksTreeModel.ROOT, no });

        String full = BookmarksUtil.computeNewBookmarkFullName("TopChild", path);
        assertEquals("TopChild", full);
    }

    @Test
    public void newBookmarkAsChild_whenParentSelected() {
        BookmarksTreeModel model = new BookmarksTreeModel();
        model.bookmarks = new TreeSet<>(Arrays.asList("Parent>>>Child1", "Parent>>>Child2"));

        Object parent = model.getChild(BookmarksTreeModel.ROOT, 1);
        TreePath path = new TreePath(new Object[] { BookmarksTreeModel.ROOT, parent });

        String full = BookmarksUtil.computeNewBookmarkFullName("NewKid", path);
        assertEquals("Parent>>>NewKid", full);
    }

    @Test
    public void newBookmarkWithSlash_whenParentSelected() {
        BookmarksTreeModel model = new BookmarksTreeModel();
        model.bookmarks = new TreeSet<>(Arrays.asList("Parent>>>Child1", "Parent>>>Child2"));

        Object parent = model.getChild(BookmarksTreeModel.ROOT, 1);
        TreePath path = new TreePath(new Object[] { BookmarksTreeModel.ROOT, parent });

        String full = BookmarksUtil.computeNewBookmarkFullName("New/Kid", path);
        assertEquals("Parent>>>New%2FKid", full);
    }

}
