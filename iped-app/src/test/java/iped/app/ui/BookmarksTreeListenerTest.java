package iped.app.ui;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JTree;
import javax.swing.tree.TreePath;

import org.junit.Test;

public class BookmarksTreeListenerTest {

    @Test
    public void testParentSelectionKeepsNewChildrenAfterRebuild() {
        // initial model with a single child under Parent
        BookmarksTreeModel model = new BookmarksTreeModel();
        model.bookmarks = new TreeSet<>(Arrays.asList("Parent>>>Child1"));

        JTree tree = new JTree(model);

        // attach tree to app singletons used by the listener
        App.get().bookmarksTree = tree;

        BookmarksTreeListener listener = new BookmarksTreeListener();
        App.get().bookmarksListener = listener;

        // simulate selection of the Parent node by placing the TreePath into the
        // listener.selection set directly (avoids triggering UI-related side
        // effects in valueChanged)
        Object parentNode = model.getChild(BookmarksTreeModel.ROOT, 1);
        TreePath parentPath = new TreePath(new Object[] { BookmarksTreeModel.ROOT, parentNode });
        try {
            java.lang.reflect.Field sel = BookmarksTreeListener.class.getDeclaredField("selection");
            sel.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Set<TreePath> s = (java.util.Set<TreePath>) sel.get(listener);
            s.add(parentPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Set<String> selBefore = listener.getSelectedBookmarkNames();
        assertTrue(selBefore.contains("Parent>>>Child1"));
        assertEquals(1, selBefore.size());

        // add a new child bookmark under the same parent and rebuild model
        model.bookmarks.add("Parent>>>Child2");

        // ensure the filtersPanel won't cause side-effects when the listener
        // notifies the combined filterer at the end of updateModelAndSelection
        App.get().filtersPanel = new FiltersPanel() {
            @Override
            public iped.app.ui.filterdecisiontree.CombinedFilterer getCombinedFilterer() {
                return new iped.app.ui.filterdecisiontree.CombinedFilterer() {
                    @Override
                    public void startSearchResult(iped.search.IMultiSearchResult input) {
                        // no-op for tests
                    }
                };
            }
        };

        // call the same remapping logic used by the UI when bookmarks change
        listener.updateModelAndSelection();

        Set<String> selAfter = listener.getSelectedBookmarkNames();
        // after rebuild the parent selection should include both children
        assertTrue(selAfter.contains("Parent>>>Child1"));
        assertTrue(selAfter.contains("Parent>>>Child2"));
        assertEquals(2, selAfter.size());
    }

}
