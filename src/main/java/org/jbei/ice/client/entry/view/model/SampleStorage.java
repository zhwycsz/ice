package org.jbei.ice.client.entry.view.model;

import java.util.LinkedList;
import java.util.List;

import org.jbei.ice.shared.dto.SampleInfo;
import org.jbei.ice.shared.dto.StorageInfo;

/**
 * A wrapper for sample info with a bunch of storage data
 * 
 * Backend model of using a storage hierarchy makes things difficult to work with
 * 
 * @author Hector Plahar
 */
public class SampleStorage {
    private SampleInfo sample;
    private LinkedList<StorageInfo> storageList;

    public SampleStorage(SampleInfo sample, List<StorageInfo> storage) {
        this.sample = sample;
        this.storageList = new LinkedList<StorageInfo>();
        if (storage != null)
            this.storageList.addAll(storage);
    }

    public SampleInfo getSample() {
        return sample;
    }

    public LinkedList<StorageInfo> getStorageList() {
        return storageList;
    }
}
