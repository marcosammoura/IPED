package iped.app.ui;

import java.text.Collator;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class BookmarksTreeModel implements TreeModel {

    public static final String ROOT_NAME = Messages.getString("BookmarksTreeModel.RootName"); //$NON-NLS-1$
    public static final String NO_BOOKMARKS_NAME = Messages.getString("BookmarksTreeModel.NoBookmarks"); //$NON-NLS-1$

    public static final BookmarkRoot ROOT = new BookmarkRoot();
    public static final NoBookmarks NO_BOOKMARKS = new NoBookmarks();
    public static final String SEPARATOR = ">>>";

    public Set<String> bookmarks;

    private final boolean includeNoBookmarks;

    private static class BookmarkRoot {
        public String toString() {
            return ROOT_NAME;
        }
    }

    public BookmarksTreeModel() {
        this(true);
    }

    public BookmarksTreeModel(boolean includeNoBookmarks) {
        this.includeNoBookmarks = includeNoBookmarks;
    }

    public Object getNodeForFullPath(String fullPath) {
        ensureTree();
        return findNodeByFullPath(parsedRoot, fullPath);
    }

    private Object findNodeByFullPath(BookmarkNode cur, String fullPath) {
        if (cur.fullPath != null && cur.fullPath.equals(fullPath))
            return cur;
        for (BookmarkNode c : cur.children) {
            Object found = findNodeByFullPath(c, fullPath);
            if (found != null)
                return found;
        }
        return null;
    }

    public Set<String> collectAllFullPaths(Object nodeObj) {
        Set<String> ret = new TreeSet<>();
        if (nodeObj == null)
            return ret;
        if (nodeObj.equals(ROOT) || nodeObj.equals(NO_BOOKMARKS))
            return ret;
        ensureTree();
        if (nodeObj instanceof String) {
            String s = (String) nodeObj;
            if (bookmarks.contains(s))
                ret.add(s);
            return ret;
        }

        if (nodeObj instanceof BookmarkNode) {
            collect((BookmarkNode) nodeObj, ret);
            return ret;
        }

        String s = nodeObj.toString();
        if (bookmarks.contains(s))
            ret.add(s);
        return ret;
    }

    /**
     * Collect all full bookmark paths represented by the given TreePath. TreePath
     * should include ROOT as the first segment; method will navigate the model
     * by segment names and collect all descendant full paths.
     */
    public Set<String> collectAllFullPaths(TreePath treePath) {
        if (treePath == null)
            return new TreeSet<>();
        Object[] path = treePath.getPath();
        if (path.length == 0)
            return new TreeSet<>();
        if (path.length == 1) // ROOT only
            return new TreeSet<>();
        Object last = path[path.length - 1];
        // if it is the NO_BOOKMARKS sentinel
        if (last == NO_BOOKMARKS)
            return new TreeSet<>();

        // build segments excluding ROOT
        String[] segments = new String[path.length - 1];
        for (int i = 1; i < path.length; i++) {
            segments[i - 1] = path[i].toString();
        }

        // try to find the corresponding node chain in the current model
        List<Object> nodePath = getNodePathForSegments(segments);
        if (nodePath != null) {
            // last node in the nodePath is the node we want to collect from
            Object lastNode = nodePath.get(nodePath.size() - 1);
            return collectAllFullPaths(lastNode);
        }

        // fallback: if the last path component is a full bookmark name in the set
        String possibleFull = segments[segments.length - 1];
        Set<String> res = new TreeSet<>();
        if (bookmarks.contains(possibleFull)) {
            res.add(possibleFull);
        }
        return res;
    }

    /**
     * Find a node matching the given segment chain and return the chain of nodes
     * from the top-level down to the matching node (excluding the ROOT object).
     * Segments should be an array of node names (the display name for each level).
     * Returns null when no matching chain is found.
     */
    public List<Object> getNodePathForSegments(String[] segments) {
        ensureTree();
        BookmarkNode cur = parsedRoot;
        List<Object> path = new ArrayList<>();
        for (String seg : segments) {
            BookmarkNode child = cur.getChildByName(seg);
            if (child == null)
                return null;
            path.add(child);
            cur = child;
        }
        return path;
    }

    private void collect(BookmarkNode node, Set<String> out) {
        if (node.fullPath != null)
            out.add(node.fullPath);
        for (BookmarkNode c : node.children)
            collect(c, out);
    }

    private static class BookmarkNode {
        final String name; // display name (last segment)
        String fullPath; // full bookmark path (non-null if node represents an actual bookmark)
        final TreeSet<BookmarkNode> children = new TreeSet<>((a, b) -> Collator.getInstance().compare(a.name, b.name));

        BookmarkNode(String name, String fullPath) {
            this.name = name;
            this.fullPath = fullPath;
        }

        @Override
        public String toString() {
            return name;
        }

        boolean isLeaf() {
            return children.isEmpty();
        }

        void addChild(BookmarkNode child) {
            children.remove(child);
            children.add(child);
        }

        BookmarkNode getChildByName(String childName) {
            for (BookmarkNode c : children) {
                if (c.name.equals(childName))
                    return c;
            }
            return null;
        }
    }

    private BookmarkNode parsedRoot;

    private static class NoBookmarks {
        public String toString() {
            return NO_BOOKMARKS_NAME;
        }
    }

    @Override
    public Object getRoot() {
        return ROOT;
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (ROOT.equals(parent)) {
            ensureTree();
            // Optionally include NO_BOOKMARKS as the first child
            if (includeNoBookmarks) {
                if (index == 0)
                    return NO_BOOKMARKS;
                // return top-level child nodes (shift by one because sentinel occupies index 0)
                List<BookmarkNode> top = new ArrayList<>(parsedRoot.children);
                return top.get(index - 1);
            } else {
                // no sentinel -> top-level nodes start at index 0
                List<BookmarkNode> top = new ArrayList<>(parsedRoot.children);
                return top.get(index);
            }
        }

        if (parent instanceof BookmarkNode) {
            BookmarkNode parentNode = (BookmarkNode) parent;
            List<BookmarkNode> chil = new ArrayList<>(parentNode.children);
            return chil.get(index);
        }

        return null;

    }

    @Override
    public int getChildCount(Object parent) {
        if (ROOT.equals(parent)) {
            // allow functionality in unit tests when bookmarks are pre-populated
            if ((bookmarks == null || bookmarks.isEmpty()) && (App.get() == null || App.get().appCase == null)) {
                return 0;
            }
            ensureTree();
            // top-level nodes +/- NO_BOOKMARKS sentinel
            return parsedRoot.children.size() + (includeNoBookmarks ? 1 : 0);
        } else if (parent instanceof BookmarkNode) {
            BookmarkNode node = (BookmarkNode) parent;
            return node.children.size();
        }
        return 0;
    }

    @Override
    public boolean isLeaf(Object node) {
        if (ROOT.equals(node))
            return false;

        if (node instanceof BookmarkNode) {
            return ((BookmarkNode) node).isLeaf();
        }

        // NO_BOOKMARKS or unknown objects are leafs
        return true;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (ROOT.equals(parent)) {
            if (child == NO_BOOKMARKS)
                return 0;
            ensureTree();
            List<BookmarkNode> top = new ArrayList<>(parsedRoot.children);
            for (int i = 0; i < top.size(); i++) {
                if (top.get(i).equals(child))
                    return i + (includeNoBookmarks ? 1 : 0);
            }
            return -1;
        }

        if (parent instanceof BookmarkNode && child instanceof BookmarkNode) {
            BookmarkNode p = (BookmarkNode) parent;
            List<BookmarkNode> list = new ArrayList<>(p.children);
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).equals(child))
                    return i;
            }
        }
        return -1;
    }

    private void ensureTree() {
        parsedRoot = new BookmarkNode("", null);
        // If bookmarks already set externally (e.g., injected in tests), use it.
        if (bookmarks == null || bookmarks.isEmpty()) {
            if (App.get() != null && App.get().appCase != null)
                bookmarks = new TreeSet<>(App.get().appCase.getMultiBookmarks().getBookmarkSet());
            else
                bookmarks = new TreeSet<>();
        }

        for (String full : bookmarks) {
            String[] parts = full.split(Pattern.quote(SEPARATOR));
            BookmarkNode cur = parsedRoot;
            StringBuilder path = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (path.length() > 0)
                    path.append('/');
                path.append(part);
                BookmarkNode child = cur.getChildByName(part);
                if (child == null) {
                    String nodeFull = (i == parts.length - 1) ? full : null;
                    child = new BookmarkNode(part, nodeFull);
                    cur.addChild(child);
                } else if (i == parts.length - 1) {
                    child.fullPath = full;
                }
                cur = child;
            }
        }
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        // treeModelListeners.addElement(l);

    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        // treeModelListeners.removeElement(l);

    }

}
