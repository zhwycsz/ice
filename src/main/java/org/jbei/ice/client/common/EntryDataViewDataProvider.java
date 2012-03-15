package org.jbei.ice.client.common;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jbei.ice.client.AppController;
import org.jbei.ice.client.RegistryServiceAsync;
import org.jbei.ice.client.common.table.DataTable;
import org.jbei.ice.shared.ColumnField;
import org.jbei.ice.shared.dto.EntryInfo;

import com.google.gwt.user.cellview.client.ColumnSortEvent.AsyncHandler;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.Range;

// Takes care of retrieving all data page, by page
public class EntryDataViewDataProvider extends AsyncDataProvider<EntryInfo> implements
        IHasNavigableData {

    protected final List<Long> valuesIds;
    protected LinkedList<EntryInfo> results;
    private final RegistryServiceAsync service;
    private final DataTable<EntryInfo> table;
    private ColumnField lastSortField;
    private boolean lastSortAsc = false;

    public EntryDataViewDataProvider(DataTable<EntryInfo> view, List<Long> data,
            RegistryServiceAsync service) {

        this.table = view;
        this.service = service;
        this.valuesIds = new LinkedList<Long>();
        results = new LinkedList<EntryInfo>();

        this.table.addColumnSortHandler(new AsyncHandler(this.table));
        DataTable<EntryInfo>.DataTableColumn<?> defaultSortField = this.table
                .getColumn(ColumnField.CREATED);

        if (defaultSortField != null) {
            ColumnSortInfo info = new ColumnSortList.ColumnSortInfo(defaultSortField, lastSortAsc);
            this.table.getColumnSortList().push(info);
        }

        if (!data.isEmpty())
            this.setValues(data);

        this.addDataDisplay(this.table);
    }

    public EntryDataViewDataProvider(DataTable<EntryInfo> view, RegistryServiceAsync service) {
        this(view, new LinkedList<Long>(), service);
    }

    // experimental. assumes that we have the entryId cached if 
    // user can click on it. performance is terrible. need a better data structure
    // expect this to be called only once and getNext()/getPrev() used instead

    @Override
    public EntryInfo getCachedData(long entryId) {
        for (EntryInfo result : results) {

            if (result.getId() == entryId)
                return result;
        }
        return null;
    }

    @Override
    public int indexOfCached(EntryInfo info) {
        return results.indexOf(info);
    }

    @Override
    public EntryInfo getNext(EntryInfo info) {
        int idx = results.indexOf(info);
        // TODO : need to check valuesIds also
        // we just may have reached the end of the cache and need to retrieve more
        if (idx == -1 || results.size() <= idx + 1)
            return null;

        // TODO : if we are at the end of results and there is still more, fetch Data
        return results.get(idx + 1);
    }

    @Override
    public EntryInfo getPrev(EntryInfo info) {
        int idx = results.indexOf(info);
        if (idx == -1)
            return null;

        return results.get(idx - 1);
    }

    @Override
    public int getSize() {
        return valuesIds.size();
    }

    public void reset() {
        this.results.clear();
        this.valuesIds.clear();
        this.table.setVisibleRangeAndClearData(table.getVisibleRange(), false);

        // reset sort 
        lastSortAsc = false;
        lastSortField = null;

        this.table.getColumnSortList().clear();
        DataTable<EntryInfo>.DataTableColumn<?> defaultSortField = this.table
                .getColumn(ColumnField.CREATED);

        if (defaultSortField != null) {
            ColumnSortInfo info = new ColumnSortList.ColumnSortInfo(defaultSortField, lastSortAsc);
            this.table.getColumnSortList().push(info);
        }
    }

    public void setValues(List<Long> data) {
        reset();

        this.valuesIds.addAll(data);
        updateRowCount(data.size(), true);

        // retrieve the first page of results and updateRowData
        final Range range = this.getRanges()[0];
        final int rangeStart = range.getStart();
        final int rangeEnd;

        if ((rangeStart + range.getLength()) > valuesIds.size())
            rangeEnd = valuesIds.size();
        else
            rangeEnd = (rangeStart + range.getLength());

        // this will always cause a sort since reset() is call.
        sort(rangeStart, rangeEnd);
    }

    public void refresh() {
        table.setVisibleRangeAndClearData(table.getVisibleRange(), true);
        updateRowCount(this.valuesIds.size(), true); // tODO : this may need to go after the call to updateRowData() in onRangeChanged
    }

    @Override
    protected void onRangeChanged(final HasData<EntryInfo> display) {

        // values of range to display from view
        final Range range = display.getVisibleRange();
        final int rangeStart = range.getStart();
        final int rangeEnd = (rangeStart + range.getLength()) > valuesIds.size() ? valuesIds.size()
                : (rangeStart + range.getLength());

        if (sort(rangeStart, rangeEnd))
            return;

        // did not need to sort so use the cache
        ArrayList<EntryInfo> show = new ArrayList<EntryInfo>();
        show.addAll(results.subList(rangeStart, rangeEnd));
        updateRowData(rangeStart, show);
    }

    /**
     * Determines if the sort params have changed and therefore warrants a
     * call to retrieve new data based on those params. Note that the rpc is
     * still made if the cache does not contain enough data;
     * 
     * @param rangeStart
     *            data range start (based on page user is on)
     * @param rangeEnd
     * @return true if the data is sorted/rpc is made
     */
    protected boolean sort(int rangeStart, int rangeEnd) {

        ColumnSortList sortList = this.table.getColumnSortList();
        final boolean sortAsc;
        final ColumnField sortField;

        sortAsc = sortList.get(0).isAscending();

        int colIndex = this.table.getColumns().indexOf(sortList.get(0).getColumn());
        if (colIndex < 0)
            sortField = lastSortField;
        else
            sortField = this.table.getColumns().get(colIndex).getField();

        // check whether we need to sort in order to determine whether we can use the cache or not
        // this is done because sort() is also called when we are paging (from onRangeChanged)
        if (lastSortAsc == sortAsc && lastSortField == sortField) {
            // make sure there is enough data in the cache for the callee to obtain what they need
            // based on range
            if (results.size() >= rangeEnd)
                return false;
        }

        results.clear();
        lastSortAsc = sortAsc;
        lastSortField = sortField;
        fetchEntryData(sortField, sortAsc, rangeStart, rangeEnd);
        return true;
    }

    /**
     * Fetches the data user is interested in viewing (usually a page)
     * 
     * @param field
     * @param ascending
     * @param rangeStart
     * @param rangeEnd
     */
    protected void fetchEntryData(ColumnField field, boolean ascending, final int rangeStart,
            final int rangeEnd) {

        if (valuesIds == null || valuesIds.isEmpty())
            return;

        // TODO : index out of bounds exception here when we page to the last page and sort
        // TODO : this is because we clear results and when we do not retrieve enough (factor below) 
        // TODO : solution is to go to page one when user sorts

        int factor = (rangeEnd - rangeStart) * 9; // get nine pages in advance
        factor = (factor + rangeEnd) > valuesIds.size() ? valuesIds.size() : (factor + rangeEnd);
        List<Long> subList = valuesIds.subList(rangeStart, factor);
        final ArrayList<Long> realValues = new ArrayList<Long>(subList);

        service.retrieveEntryData(AppController.sessionId, realValues, field, ascending,
            new AsyncCallback<ArrayList<EntryInfo>>() {

                @Override
                public void onFailure(Throwable caught) {
                    Window.alert(caught.getMessage());
                }

                @Override
                public void onSuccess(ArrayList<EntryInfo> result) {

                    results.addAll(result);
                    int end = rangeEnd;
                    if (rangeEnd > results.size())
                        end = results.size();
                    updateRowData(rangeStart, results.subList(rangeStart, end));
                }
            });
    }
}
