package iped.app.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import org.roaringbitmap.RoaringBitmap;

import iped.data.IMultiBookmarks;
import iped.engine.data.MultiBitmapBookmarks;
import iped.engine.search.MultiSearchResult;
import iped.exception.ParseException;
import iped.exception.QueryNodeException;
import iped.search.IMultiSearchResult;
import iped.viewers.api.IBitmapFilter;
import iped.viewers.api.IFilter;
import iped.viewers.api.IMutableFilter;
import iped.viewers.api.IResultSetFilter;
import iped.viewers.api.IResultSetFilterer;

public class BookmarksTreeListener implements TreeSelectionListener, TreeExpansionListener, IResultSetFilterer {
    private HashSet<TreePath> selection = new HashSet<>();
    private volatile boolean updatingSelection = false;
    private long collapsed = 0;
    private boolean clearing = false;

    HashMap<TreePath, IFilter> definedFilters = new HashMap<TreePath, IFilter>();

    public Set<String> getSelectedBookmarkNames() {
        BookmarksTreeModel model = (BookmarksTreeModel) App.get().bookmarksTree.getModel();
        Set<String> result = new HashSet<>();
        for (TreePath sel : selection) {
            Object lastPathComponent = sel.getLastPathComponent();
            if (lastPathComponent == BookmarksTreeModel.ROOT || lastPathComponent == BookmarksTreeModel.NO_BOOKMARKS)
                continue;
            result.addAll(model.collectAllFullPaths(sel));
        }
        return result;
    }

    public boolean isRootSelected() {
        for (TreePath tp : selection) {
            if (tp.getPathCount() == 1 && tp.getLastPathComponent() == BookmarksTreeModel.ROOT)
                return true;
        }
        return false;
    }

    public boolean isNoBookmarksSelected() {
        for (TreePath tp : selection) {
            if (tp.getPathCount() == 2 && tp.getLastPathComponent() == BookmarksTreeModel.NO_BOOKMARKS)
                return true;
        }
        return false;
    }

    @Override
    public void valueChanged(TreeSelectionEvent evt) {
        if (updatingSelection) {
            return;
        }

        if (System.currentTimeMillis() - collapsed < 500) {
            if (evt.getPath().getLastPathComponent().equals(BookmarksTreeModel.ROOT)) {
                updateModelAndSelection();
            }
            return;
        }

        for (TreePath path : evt.getPaths()) {
            if (selection.contains(path)) {
                selection.remove(path);
                definedFilters.remove(path);
            } else {
                selection.add(path);
                Object lastPathComponent = path.getLastPathComponent();
                if (lastPathComponent != BookmarksTreeModel.ROOT && lastPathComponent != BookmarksTreeModel.NO_BOOKMARKS) {
                    BookmarksTreeModel model = (BookmarksTreeModel) App.get().bookmarksTree.getModel();
                    HashSet<String> bookmarkNames = new HashSet<>(model.collectAllFullPaths(path));
                    if (!bookmarkNames.isEmpty())
                        definedFilters.put(path, new BookMarkFilter(bookmarkNames));
                }
            }
        }

        if (!clearing)
            App.get().appletListener.updateFileListing();

        if (selection.contains(BookmarksTreeModel.ROOT) || selection.isEmpty()) {
            App.get().setBookmarksDefaultColor(true);
        } else {
            App.get().setBookmarksDefaultColor(false);
        }

    }

    public void updateModelAndSelection() {

        updatingSelection = true;
        Set<String> bookmarkSet = ((BookmarksTreeModel) App.get().bookmarksTree.getModel()).bookmarks;

        if (bookmarkSet != null && !selection.isEmpty()) {

            HashSet<TreePath> tempSel = new HashSet<>(selection);
            selection.clear();
            definedFilters.clear();

            List<TreePath> needToRestorePaths = new ArrayList<>();
            boolean hadRoot = false;
            boolean hadNoBookmarks = false;

            for (TreePath tp : tempSel) {
                Object lastPathComponent = tp.getLastPathComponent();
                if (lastPathComponent == BookmarksTreeModel.ROOT) {
                    hadRoot = true;
                    continue;
                }
                if (lastPathComponent == BookmarksTreeModel.NO_BOOKMARKS) {
                    hadNoBookmarks = true;
                    continue;
                }

                needToRestorePaths.add(tp);
            }

            ArrayList<TreePath> selectedPaths = new ArrayList<TreePath>();
            if (hadRoot)
                selectedPaths.add(new TreePath(new Object[] { BookmarksTreeModel.ROOT }));
            if (hadNoBookmarks)
                selectedPaths.add(new TreePath(new Object[] { BookmarksTreeModel.ROOT, BookmarksTreeModel.NO_BOOKMARKS }));

            boolean rootCollapsed = App.get().bookmarksTree.isCollapsed(0);
            BookmarksTreeModel newModel = new BookmarksTreeModel();
            if (bookmarkSet != null)
                newModel.bookmarks = new TreeSet<>(bookmarkSet);
            App.get().bookmarksTree.setModel(newModel);
            if (rootCollapsed) {
                App.get().bookmarksTree.collapseRow(0);
            }

            for (TreePath oldTp : needToRestorePaths) {
                Object[] parts = oldTp.getPath();
                String[] segments = new String[parts.length - 1];
                for (int i = 1; i < parts.length; i++)
                    segments[i - 1] = parts[i].toString();

                List<Object> newPath = newModel.getNodePathForSegments(segments);
                if (newPath != null) {
                    Object[] tpObjs = new Object[1 + newPath.size()];
                    tpObjs[0] = BookmarksTreeModel.ROOT;
                    for (int i = 0; i < newPath.size(); i++)
                        tpObjs[i + 1] = newPath.get(i);

                    TreePath newTp = new TreePath(tpObjs);
                    selection.add(newTp);
                    Set<String> all = new TreeSet<>();
                    all.addAll(newModel.collectAllFullPaths(newTp));
                    if (!all.isEmpty())
                        definedFilters.put(newTp, new BookMarkFilter(all));
                    selectedPaths.add(newTp);
                } else {
                    // fallback: maybe the old last item represented a full path string
                    String possibleFull = segments[segments.length - 1];
                    Object node = newModel.getNodeForFullPath(possibleFull);
                    if (node != null) {
                        TreePath newTp = new TreePath(new Object[] { BookmarksTreeModel.ROOT, node });
                        selection.add(newTp);
                        HashSet<String> oneBookmark = new HashSet<>();
                        oneBookmark.add(possibleFull);
                        definedFilters.put(newTp, new BookMarkFilter(oneBookmark));
                        selectedPaths.add(newTp);
                    }
                }
            }

            App.get().bookmarksTree.setSelectionPaths(selectedPaths.toArray(new TreePath[0]));

        } else {
            boolean rootCollapsed = App.get().bookmarksTree.isCollapsed(0);
            BookmarksTreeModel newModel = new BookmarksTreeModel();
            if (bookmarkSet != null)
                newModel.bookmarks = new TreeSet<>(bookmarkSet);
            App.get().bookmarksTree.setModel(newModel);
            if (rootCollapsed) {
                App.get().bookmarksTree.collapseRow(0);
            }
        }

        // informs combinedfilter of bookmark change so it can update its internal
        // bitset cache
        App.get().filtersPanel.getCombinedFilterer().startSearchResult(App.get().ipedResult);

        updatingSelection = false;
    }

    @Override
    public void treeExpanded(TreeExpansionEvent event) {

    }

    @Override
    public void treeCollapsed(TreeExpansionEvent event) {
        collapsed = System.currentTimeMillis();

    }

    @Override
    public void clearFilter() {
        clearing = true;
        App.get().bookmarksTree.clearSelection();
        selection.clear();
        definedFilters.clear();
        clearing = false;
    }

    NoBookMarkFilter noBookMarkFilter = new NoBookMarkFilter();

    @Override
    public List getDefinedFilters() {
        ArrayList<IFilter> result = new ArrayList<IFilter>();
        Set<String> bookmarkSelection = getSelectedBookmarkNames();
        if ((!bookmarkSelection.isEmpty() || isNoBookmarksSelected()) && !isRootSelected()) {
            if ((!bookmarkSelection.isEmpty() || isNoBookmarksSelected()) && !isRootSelected()) {
                result.addAll(definedFilters.values());

                if (isNoBookmarksSelected()) {
                    result.add(noBookMarkFilter);
                }
            }
        }
        return result;
    }

    public String toString() {
        return "Bookmarks filterer";
    }

    @Override
    public IFilter getFilter() {
        Set<String> bookmarkSelection = getSelectedBookmarkNames();

        if ((!bookmarkSelection.isEmpty() || isNoBookmarksSelected()) && !isRootSelected()) {
            IFilter result;
            if (isNoBookmarksSelected()) {
                if (bookmarkSelection.isEmpty()) {
                    result = noBookMarkFilter;
                } else {
                    result = new NoBookMarkFilter(bookmarkSelection);
                }
            } else {
                result = new BookMarkFilter(bookmarkSelection);
            }
            return result;
        }
        return null;
    }

    @Override
    public boolean hasFilters() {
        return selection.size() > 0 && !isRootSelected();
    }

    @Override
    public boolean hasFiltersApplied() {
        return selection.size() > 0 && !isRootSelected();
    }
}

class BookMarkFilter implements IResultSetFilter, IMutableFilter, IBitmapFilter {
    Set<String> bookmark;

    public BookMarkFilter(Set<String> bookmark2) {
        this.bookmark = bookmark2;
    }

    @Override
    public IMultiSearchResult filterResult(IMultiSearchResult src) throws ParseException, QueryNodeException, IOException {
        return (MultiSearchResult) App.get().appCase.getMultiBookmarks().filterBookmarks(src, (Set<String>) bookmark);
    }

    public String toString() {
        return bookmark.toString();
    }

    @Override
    public RoaringBitmap[] getBitmap() {
        IMultiBookmarks mm = App.get().appCase.getMultiBookmarks();
        if (mm instanceof MultiBitmapBookmarks) {
            return ((MultiBitmapBookmarks) mm).getBookmarksUnions(bookmark);
        }
        return null;
    }
};

class NoBookMarkFilter implements IResultSetFilter, IMutableFilter, IBitmapFilter {
    Set<String> bookmarkSelection;

    public NoBookMarkFilter(Set<String> bookmarkSelection) {
        this.bookmarkSelection = bookmarkSelection;
    }

    public NoBookMarkFilter() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public IMultiSearchResult filterResult(IMultiSearchResult src) throws ParseException, QueryNodeException, IOException {
        return (MultiSearchResult) App.get().appCase.getMultiBookmarks().filterNoBookmarks(src);
    }

    public String toString() {
        return BookmarksTreeModel.NO_BOOKMARKS_NAME;
    }

    @Override
    public boolean isToFilterOut() {
        return true;
    };

    @Override
    public RoaringBitmap[] getBitmap() {
        IMultiBookmarks mm = App.get().appCase.getMultiBookmarks();
        if (mm instanceof MultiBitmapBookmarks) {
            RoaringBitmap[] result = ((MultiBitmapBookmarks) mm).getBookmarksUnions();
            RoaringBitmap[] rbSelecitons = null;
            if (bookmarkSelection != null) {
                rbSelecitons = ((MultiBitmapBookmarks) mm).getBookmarksUnions(bookmarkSelection);
            }
            if (rbSelecitons != null) {
                for (int i = 0; i < result.length; i++) {
                    result[i].andNot(rbSelecitons[i]);
                }
            }
            return result;
        }
        return null;
    }
}
