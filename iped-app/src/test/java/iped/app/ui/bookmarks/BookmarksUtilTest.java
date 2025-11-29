package iped.app.ui.bookmarks;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import iped.app.ui.BookmarksUtil;

public class BookmarksUtilTest {

    @Test
    public void concatLeaf_singleFullPath() {
        String out = BookmarksUtil.concatLeafNames(Arrays.asList("Parent>>>Child"));
        assertEquals("Child", out);
    }

    @Test
    public void concatLeaf_multiplePaths() {
        String out = BookmarksUtil.concatLeafNames(Arrays.asList("A>>>B>>>C", "Top"));
        assertEquals("C | Top", out);
    }

    @Test
    public void concatLeaf_emptyList() {
        String out = BookmarksUtil.concatLeafNames(Arrays.asList());
        assertEquals("", out);
    }

    @Test
    public void concatLeaf_nullList() {
        String out = BookmarksUtil.concatLeafNames(null);
        assertNull(out);
    }

}
