package iped.app.ui;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

public class BookmarksTreeModelTest {

    @Test
    public void testHierarchyBuildingAndLookup() {
        BookmarksTreeModel model = new BookmarksTreeModel();
        // prepare a simple hierarchical bookmark set
        model.bookmarks = new TreeSet<>(Arrays.asList("A>>>B>>>C", "A>>>B>>>D", "E"));

        // root children: NO_BOOKMARKS + top-level nodes (A, E)
        int rootCount = model.getChildCount(BookmarksTreeModel.ROOT);
        assertEquals(3, rootCount);

        Object first = model.getChild(BookmarksTreeModel.ROOT, 1);
        assertNotNull(first);
        // top-level node name
        assertEquals("A", first.toString());

        Set<String> collected = model.collectAllFullPaths(first);
        assertTrue(collected.contains("A>>>B>>>C"));
        assertTrue(collected.contains("A>>>B>>>D"));

        Object node = model.getNodeForFullPath("A>>>B>>>C");
        assertNotNull(node);

        // also accept legacy '/' based stored paths
        model.bookmarks = new TreeSet<>(Arrays.asList("X/Y/Z", "X/Y/W"));
        ensureTreeAgain(model);
        Object firstLegacy = model.getChild(BookmarksTreeModel.ROOT, 1);
        assertNotNull(firstLegacy);
        assertEquals("X", firstLegacy.toString());
        Set<String> collectedLegacy = model.collectAllFullPaths(firstLegacy);
        assertTrue(collectedLegacy.contains("X/Y/Z"));
        assertTrue(collectedLegacy.contains("X/Y/W"));
    }

    private void ensureTreeAgain(BookmarksTreeModel model) {
        // force rebuild by invoking getChildCount which calls ensureTree
        model.getChildCount(BookmarksTreeModel.ROOT);
    }

}
