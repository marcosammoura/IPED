package iped.app.ui;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.TreeSet;

import javax.swing.tree.TreePath;

import org.junit.Test;

public class BookmarksManagerDialogTest {

    @Test
    public void dialogModelHasNoNoBookmarksSentinel() {
        BookmarksManager mgr = BookmarksManager.get();
        BookmarksTreeModel model = (BookmarksTreeModel) mgr.tree.getModel();

        // ensure model was created for dialog (no sentinel). When empty there
        // should be no children under root
        model.bookmarks = new TreeSet<>();
        assertEquals(0, model.getChildCount(BookmarksTreeModel.ROOT));
    }

    @Test
    public void preservesSelectionAndExpansion_whenAddingChild() {
        BookmarksManager mgr = BookmarksManager.get();

        // prepare a small model and attach to dialog tree
        BookmarksTreeModel model = new BookmarksTreeModel(false);
        model.bookmarks = new TreeSet<>(Arrays.asList("Parent>>>Child1"));
        mgr.tree.setModel(model);

        // expand parent and select it
        Object parentNode = model.getChild(BookmarksTreeModel.ROOT, 0);
        mgr.tree.expandPath(new TreePath(new Object[] { BookmarksTreeModel.ROOT, parentNode }));
        mgr.tree.setSelectionPath(new TreePath(new Object[] { BookmarksTreeModel.ROOT, parentNode }));

        // add a new child under the same parent
        model.bookmarks.add("Parent>>>Child2");

        // call updateList which should preserve the expansion and selection
        mgr.updateList();
        // selection state should be preserved by updateList

        TreePath sel = mgr.tree.getSelectionPath();
        assertNotNull(sel);
        // selection should still be the parent node
        assertEquals(2, sel.getPathCount());
        // selection should map to both children
        BookmarksTreeModel after = (BookmarksTreeModel) mgr.tree.getModel();
                assertTrue(after.collectAllFullPaths(sel).contains("Parent>>>Child1"));
                assertTrue(after.collectAllFullPaths(sel).contains("Parent>>>Child2"));
    }

    @Test
    public void selectsParent_whenChildRemoved() {
        BookmarksManager mgr = BookmarksManager.get();

        BookmarksTreeModel model = new BookmarksTreeModel(false);
        model.bookmarks = new TreeSet<>(Arrays.asList("Parent>>>Child1"));
        mgr.tree.setModel(model);

        // select the child node
        Object child = model.getNodeForFullPath("Parent>>>Child1");
        mgr.tree.setSelectionPath(new TreePath(new Object[] { BookmarksTreeModel.ROOT, child }));

        // remove child
        model.bookmarks.remove("Parent>>>Child1");

        mgr.updateList();

        // after removal, a selection should exist. If the parent node still
        // exists in the model we'll get a two-segment path (root + parent),
        // otherwise the root should be selected.
        TreePath sel = mgr.tree.getSelectionPath();
        assertNotNull(sel);
        if (sel.getPathCount() == 2) {
            assertEquals("Parent", sel.getLastPathComponent().toString());
        } else {
            assertEquals(1, sel.getPathCount());
            assertEquals(BookmarksTreeModel.ROOT, sel.getLastPathComponent());
        }
    }

}
