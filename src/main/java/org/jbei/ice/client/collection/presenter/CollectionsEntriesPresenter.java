package org.jbei.ice.client.collection.presenter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import org.jbei.ice.client.AbstractPresenter;
import org.jbei.ice.client.AppController;
import org.jbei.ice.client.Page;
import org.jbei.ice.client.collection.ICollectionEntriesView;
import org.jbei.ice.client.collection.add.EntryAddPresenter;
import org.jbei.ice.client.collection.event.EntryIdsEvent;
import org.jbei.ice.client.collection.event.EntryIdsEventHandler;
import org.jbei.ice.client.collection.event.FolderEvent;
import org.jbei.ice.client.collection.event.FolderEventHandler;
import org.jbei.ice.client.collection.event.FolderRetrieveEvent;
import org.jbei.ice.client.collection.event.FolderRetrieveEventHandler;
import org.jbei.ice.client.collection.menu.MenuItem;
import org.jbei.ice.client.collection.model.CollectionsModel;
import org.jbei.ice.client.collection.table.CollectionEntriesDataTable;
import org.jbei.ice.client.collection.view.OptionSelect;
import org.jbei.ice.client.common.EntryDataViewDataProvider;
import org.jbei.ice.client.common.entry.IHasEntryId;
import org.jbei.ice.client.common.table.DataTable;
import org.jbei.ice.client.common.table.EntryTablePager;
import org.jbei.ice.client.entry.view.EntryPresenter;
import org.jbei.ice.client.event.EntryViewEvent;
import org.jbei.ice.client.event.EntryViewEvent.EntryViewEventHandler;
import org.jbei.ice.client.event.FeedbackEvent;
import org.jbei.ice.client.event.SearchEvent;
import org.jbei.ice.client.event.SearchEventHandler;
import org.jbei.ice.client.event.ShowEntryListEvent;
import org.jbei.ice.client.event.ShowEntryListEventHandler;
import org.jbei.ice.client.search.advanced.AdvancedSearchPresenter;
import org.jbei.ice.shared.ColumnField;
import org.jbei.ice.shared.EntryAddType;
import org.jbei.ice.shared.FolderDetails;
import org.jbei.ice.shared.dto.EntryInfo;
import org.jbei.ice.shared.dto.SearchFilterInfo;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.cellview.client.ColumnSortEvent.AsyncHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

public class CollectionsEntriesPresenter extends AbstractPresenter {

    // this is a temporary measure to determine how to return entries both 
    // search and collection browsing. this should be replaced by EntryContext
    private enum Mode {
        SEARCH, COLLECTION;
    }

    private final ICollectionEntriesView display;

    private final EntryDataViewDataProvider entryDataProvider;
    private final CollectionEntriesDataTable collectionsDataTable;

    //  data providers for the sub menu
    private final ListDataProvider<FolderDetails> userListProvider;
    private final ListDataProvider<FolderDetails> systemListProvider;

    // selection menu
    private final CollectionsModel model;
    private AdvancedSearchPresenter searchPresenter;
    private EntryAddPresenter entryPresenter;
    private EntryPresenter entryViewPresenter;
    private long currentFolder;
    private Mode mode = Mode.COLLECTION;

    public CollectionsEntriesPresenter(CollectionsModel model,
            final ICollectionEntriesView display, ArrayList<SearchFilterInfo> operands) {
        this(model, display);
        search(operands);
    }

    // collections for entry view
    public CollectionsEntriesPresenter(CollectionsModel model, final ICollectionEntriesView view,
            EntryContext event) {
        this(model, view);
        this.showEntryView(event);
    }

    // TODO : really need to do something about the size of this constructor
    public CollectionsEntriesPresenter(final CollectionsModel model,
            final ICollectionEntriesView display) {
        this.display = display;
        this.model = model;

        // initialize all parameters
        this.collectionsDataTable = new CollectionEntriesDataTable(new EntryTablePager()) {

            @Override
            protected EntryViewEventHandler getHandler() {
                return new EntryViewEventHandler() {
                    @Override
                    public void onEntryView(EntryViewEvent event) {
                        event.setList(entryDataProvider.getData());
                        model.getEventBus().fireEvent(event);
                    }
                };
            }
        };
        this.userListProvider = new ListDataProvider<FolderDetails>(new KeyProvider());
        this.systemListProvider = new ListDataProvider<FolderDetails>(new KeyProvider());
        this.entryDataProvider = new EntryDataViewDataProvider(collectionsDataTable,
                model.getService());

        // setting sorting for collections. TODO : this should go into the data provider
        setCollectionsSort();

        // selection models used for menus
        initMenus();

        // init text box
        initCreateCollectionHandlers();

        // create entry handler
        final SingleSelectionModel<EntryAddType> selectionModel = display
                .getAddEntrySelectionHandler();
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {

            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                if (entryPresenter == null)
                    entryPresenter = new EntryAddPresenter(model.getService(), model.getEventBus());
                entryPresenter.setType(selectionModel.getSelectedObject());
                display.setMainContent(entryPresenter.getView(), false);
            }
        });

        model.getEventBus().addHandler(SearchEvent.TYPE, new SearchEventHandler() {

            @Override
            public void onSearch(SearchEvent event) {
                search(event.getFilters());
            }
        });

        // show entry context
        model.getEventBus().addHandler(ShowEntryListEvent.TYPE, new ShowEntryListEventHandler() {

            @Override
            public void onEntryListContextAvailable(ShowEntryListEvent event) {
                EntryContext context = event.getContext();
                if (context == null)
                    return;

                handleContext(context);
            }
        });

        // register for entry view events
        model.getEventBus().addHandler(EntryViewEvent.TYPE, new EntryViewEventHandler() {

            @Override
            public void onEntryView(EntryViewEvent event) {
                if (entryViewPresenter != null) {
                    History.newItem(Page.ENTRY_VIEW.getLink() + ";id="
                            + event.getContext().getCurrent(), false);
                    display.setMainContent(entryViewPresenter.getView(), false);
                    return;
                }

                showEntryView(event.getContext());
            }
        });

        model.getEventBus().addHandler(FeedbackEvent.TYPE,
            new FeedbackEvent.IFeedbackEventHandler() {

                @Override
                public void onFeedbackAvailable(FeedbackEvent event) {
                    display.showFeedbackMessage(event.getMessage(), event.isError());
                }
            });

        AddToSubmitHandler addHandler = new AddToSubmitHandler(display, new HasEntry()) {

            @Override
            public void addEntriesToFolder(final Set<Long> destinationFolders,
                    final ArrayList<Long> entryIds) {
                model.addEntriesToFolder(new ArrayList<Long>(destinationFolders), entryIds,
                    new FolderRetrieveEventHandler() {

                        @Override
                        public void onMenuRetrieval(FolderRetrieveEvent event) {
                            if (event == null || event.getItems() == null) {
                                display.showFeedbackMessage(
                                    "An error occured while adding entries. Please try again.",
                                    true);
                                return;
                            }

                            ArrayList<FolderDetails> results = event.getItems();
                            ArrayList<MenuItem> items = new ArrayList<MenuItem>();
                            for (FolderDetails result : results) {
                                items.add(new MenuItem(result.getId(), result.getName(), result
                                        .getCount(), result.isSystemFolder()));
                            }
                            display.updateMenuItemCounts(items);
                            String entryDisp = (entryIds.size() == 1) ? "entry" : "entries";
                            String msg = "<b>" + entryIds.size() + "</b> " + entryDisp
                                    + " successfully added to ";
                            msg += ("\"<b>" + results.get(0).getName() + "</b>\" collection.");
                            display.showFeedbackMessage(msg, false);
                        }
                    });
            }
        };
        display.addAddToSubmitHandler(addHandler);

        // move to handler
        MoveToSubmitHandler moveHandler = new MoveToSubmitHandler(display, new HasEntry()) {

            @Override
            protected void moveEntriesToFolder(Set<Long> destinationFolders,
                    final ArrayList<Long> entryIds) {
                // TODO : both this and "add to" use the wrong handler. this becomes more of an issue when the presenter listens on the event bus
                model.moveEntriesToFolder(currentFolder, new ArrayList<Long>(destinationFolders),
                    entryIds, new FolderRetrieveEventHandler() {

                        @Override
                        public void onMenuRetrieval(FolderRetrieveEvent event) {
                            if (event == null || event.getItems() == null) {
                                display.showFeedbackMessage(
                                    "An error occured while moving entries. Please try again.",
                                    true);
                                return;
                            }

                            ArrayList<FolderDetails> results = event.getItems();
                            ArrayList<MenuItem> items = new ArrayList<MenuItem>();
                            for (FolderDetails result : results) {
                                items.add(new MenuItem(result.getId(), result.getName(), result
                                        .getCount(), result.isSystemFolder()));
                            }
                            display.updateMenuItemCounts(items);
                            String entryDisp = (entryIds.size() == 1) ? "entry" : "entries";
                            String msg = "<b>" + entryIds.size() + "</b> " + entryDisp
                                    + " successfully moved";

                            if (results.size() == 1)
                                msg += ("\" to <b>" + results.get(0).getName() + "</b>\" collection.");
                            else
                                msg += ".";

                            retrieveEntriesForFolder(currentFolder);
                            display.showFeedbackMessage(msg, false);
                        }
                    });
            }
        };
        display.addMoveSubmitHandler(moveHandler);

        // remove handler
        ClickHandler removeHandler = new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {

                final ArrayList<Long> ids = new ArrayList<Long>(
                        new HasEntry().getSelectedEntrySet());

                model.removeEntriesFromFolder(currentFolder, ids, new FolderRetrieveEventHandler() {

                    @Override
                    public void onMenuRetrieval(FolderRetrieveEvent event) {
                        if (event == null || event.getItems() == null) {
                            display.showFeedbackMessage(
                                "An error occured while removing entries. Please try again.", true);
                            return;
                        }

                        FolderDetails result = event.getItems().get(0);
                        ArrayList<MenuItem> items = new ArrayList<MenuItem>();
                        MenuItem updateItem = new MenuItem(result.getId(), result.getName(), result
                                .getCount(), result.isSystemFolder());
                        items.add(updateItem);
                        display.updateMenuItemCounts(items);

                        String entryDisp = (ids.size() == 1) ? "entry" : "entries";
                        String msg = "<b>" + ids.size() + "</b> " + entryDisp
                                + " successfully moved to ";
                        msg += ("\"<b>" + result.getName() + "</b>\" collection.");

                        retrieveEntriesForFolder(currentFolder);
                        display.showFeedbackMessage(msg, false);
                    }
                });
            }
        };
        display.addRemoveHandler(removeHandler);
    }

    public CollectionsEntriesPresenter(CollectionsModel model,
            final ICollectionEntriesView display, String param) {

        // collection sub menu
        this(model, display);
        long id = 0;
        try {
            if (param != null)
                id = Long.decode(param);
        } catch (NumberFormatException nfe) {
            id = 0;
        }

        retrieveEntriesForFolder(id);
    }

    private void showEntryView(EntryContext event) {
        if (entryViewPresenter == null)
            entryViewPresenter = new EntryPresenter(model.getService(), model.getEventBus(), event);

        History.newItem(Page.ENTRY_VIEW.getLink() + ";id=" + event.getCurrent(), false);
        display.setMainContent(entryViewPresenter.getView(), false);
    }

    private void search(ArrayList<SearchFilterInfo> operands) {
        if (operands == null)
            return;

        if (searchPresenter == null)
            searchPresenter = new AdvancedSearchPresenter(model.getService(), model.getEventBus(),
                    operands);

        display.setMainContent(searchPresenter.getView(), true);
        mode = Mode.SEARCH;
    }

    private void handleContext(EntryContext context) {
        ArrayList<Long> ids = new ArrayList<Long>(context.getList());
        entryDataProvider.setValues(ids);
        display.setDataView(collectionsDataTable);
        //        display.setCurrentMenuSelection(id);
        //        currentFolder = id;
        mode = Mode.SEARCH;

    }

    private void initCreateCollectionHandlers() {
        this.display.setQuickAddVisibility(false);
        this.display.addQuickAddKeyHandler(new KeyPressHandler() {

            @Override
            public void onKeyPress(KeyPressEvent event) {
                if (event.getCharCode() != KeyCodes.KEY_ENTER)
                    return;

                display.setQuickAddVisibility(false);
                saveCollection(display.getCollectionInputValue());
                display.hideQuickAddInput();
            }
        });

        // quick edit
        display.addQuickEditBlurHandler(new BlurHandler() {

            @Override
            public void onBlur(BlurEvent event) {
                handle();
            }
        });

        display.addQuickEditKeyDownHandler(new KeyDownHandler() {

            @Override
            public void onKeyDown(KeyDownEvent event) {
                if (event.getNativeKeyCode() != KeyCodes.KEY_ENTER)
                    return;
                handle();
            }
        });
    }

    private void handle() {
        if (!display.getQuickEditVisibility())
            return;

        String newName = display.getQuickEditInput();
        final MenuItem item = display.getCurrentMenuEditSelection();
        if (newName.trim().equals(item.getName().trim())) {
            display.setMenuItem(item);
            return;
        }

        // RPC with newName
        FolderDetails editFolder = new FolderDetails();
        editFolder.setCount(item.getCount());
        editFolder.setName(newName);
        editFolder.setId(item.getId());

        model.updateFolder(item.getId(), editFolder, new FolderEventHandler() {

            @Override
            public void onFolderEvent(FolderEvent event) {
                FolderDetails folder = event.getFolder();
                if (folder == null) {
                    display.showFeedbackMessage("Error updating collection. Please try again",
                        false);
                    return;
                }

                display.updateSubMenuFolder(new OptionSelect(folder.getId(), folder.getName()));
                MenuItem resultItem = new MenuItem(folder.getId(), folder.getName(), folder
                        .getCount(), folder.isSystemFolder());
                display.setMenuItem(resultItem);
            }
        });
    }

    private void saveCollection(String value) {
        if (value == null || value.isEmpty())
            return;

        model.createFolder(value, new FolderEventHandler() {

            @Override
            public void onFolderEvent(FolderEvent event) {
                FolderDetails folder = event.getFolder();
                if (folder == null) {
                    display.showFeedbackMessage("Error creating new folder. Please try again", true);
                    return;
                }

                userListProvider.getList().add(folder);
                display.addSubMenuFolder(new OptionSelect(folder.getId(), folder.getName()));
                MenuItem newItem = new MenuItem(folder.getId(), folder.getName(),
                        folder.getCount(), folder.isSystemFolder());
                display.addMenuItem(newItem);
            }
        });
    }

    private void setCollectionsSort() {
        // collections table view. single view used for all collections
        collectionsDataTable.addColumnSortHandler(new AsyncHandler(collectionsDataTable));
        DataTable<EntryInfo>.DataTableColumn<?> createdField = collectionsDataTable
                .getColumn(ColumnField.CREATED);
        collectionsDataTable.getColumnSortList().push(createdField);
    }

    /**
     * Initializes the selection models used for the menu items
     * by adding the selection change handlers
     */
    private void initMenus() {
        final SingleSelectionModel<MenuItem> userModel = display.getUserMenuModel();
        final SingleSelectionModel<MenuItem> systemModel = display.getSystemMenuModel();

        userModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {

            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                MenuItem selection = userModel.getSelectedObject();
                if (selection == null)
                    return;

                retrieveEntriesForFolder(selection.getId());
            }
        });

        systemModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {

            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                MenuItem selection = systemModel.getSelectedObject();
                if (selection == null)
                    return;

                retrieveEntriesForFolder(selection.getId());
            }
        });

        // retrieve folders to use as menu
        model.retrieveFolders(new FolderRetrieveEventHandler() {

            @Override
            public void onMenuRetrieval(FolderRetrieveEvent event) {
                ArrayList<FolderDetails> folders = event.getItems();
                Collections.reverse(folders);

                ArrayList<MenuItem> userMenuItems = new ArrayList<MenuItem>();
                ArrayList<FolderDetails> userFolders = new ArrayList<FolderDetails>();

                ArrayList<MenuItem> systemMenuItems = new ArrayList<MenuItem>();
                ArrayList<FolderDetails> systemFolder = new ArrayList<FolderDetails>();

                for (FolderDetails folder : folders) {
                    MenuItem item = new MenuItem(folder.getId(), folder.getName(), folder
                            .getCount(), folder.isSystemFolder());

                    if (folder.isSystemFolder()) {
                        systemMenuItems.add(item);
                        systemFolder.add(folder);
                    } else {
                        userMenuItems.add(item);
                        userFolders.add(folder);
                        display.addSubMenuFolder(new OptionSelect(folder.getId(), folder.getName()));
                    }
                }

                // my entries
                MenuItem item = new MenuItem(0, "My Entries", AppController.accountInfo
                        .getUserEntryCount(), true);
                userMenuItems.add(0, item);
                MenuItem allEntriesItem = new MenuItem(-1, "Available Entries",
                        AppController.accountInfo.getVisibleEntryCount(), true);
                systemMenuItems.add(0, allEntriesItem);
                display.setSystemCollectionMenuItems(systemMenuItems);
                display.setUserCollectionMenuItems(userMenuItems);

                userListProvider.getList().addAll(userFolders);
                systemListProvider.getList().addAll(systemFolder);
            }
        });
    }

    private void retrieveEntriesForFolder(final long id) {

        model.retrieveEntriesForFolder(id, new EntryIdsEventHandler() {

            @Override
            public void onEntryIdsEvent(EntryIdsEvent event) {
                if (event == null || event.getIds() == null) {
                    display.showFeedbackMessage("Error connecting to server. Please try again",
                        true);
                    return;
                }

                History.newItem(Page.COLLECTIONS.getLink() + ";id=" + id, false);
                ArrayList<Long> ids = event.getIds();
                entryDataProvider.setValues(ids);
                display.setDataView(collectionsDataTable);
                display.setCurrentMenuSelection(id);
                currentFolder = id;
                mode = Mode.COLLECTION;
            }
        });
    }

    @Override
    public void go(HasWidgets container) {
        container.clear();
        container.add(this.display.asWidget());
    }

    private class KeyProvider implements ProvidesKey<FolderDetails> {

        @Override
        public Long getKey(FolderDetails item) {
            return item.getId();
        }
    }

    private class HasEntry implements IHasEntryId {

        @Override
        public Set<Long> getSelectedEntrySet() {
            switch (mode) {
            case COLLECTION:
            default:
                return collectionsDataTable.getSelectedEntrySet();

            case SEARCH:
                return searchPresenter.getEntrySet();
            }
        }
    }
}
