package voldemort.store.routed;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import voldemort.cluster.Cluster;
import voldemort.cluster.failuredetector.FailureDetector;
import voldemort.store.Store;
import voldemort.store.StoreDefinition;
import voldemort.store.nonblockingstore.NonblockingStore;
import voldemort.store.nonblockingstore.ThreadPoolBasedNonblockingStoreImpl;
import voldemort.utils.ByteArray;
import voldemort.utils.SystemTime;

import com.google.common.collect.Maps;

public class RoutedStoreFactory {

    private final boolean isPipelineRoutedStoreEnabled;

    private final ExecutorService threadPool;

    private final long routingTimeoutMs;

    public RoutedStoreFactory(boolean isPipelineRoutedStoreEnabled,
                              ExecutorService threadPool,
                              long routingTimeoutMs) {
        this.isPipelineRoutedStoreEnabled = isPipelineRoutedStoreEnabled;
        this.threadPool = threadPool;
        this.routingTimeoutMs = routingTimeoutMs;
    }

    public RoutedStore create(Cluster cluster,
                              StoreDefinition storeDefinition,
                              Map<Integer, Store<ByteArray, byte[]>> nodeStores,
                              boolean repairReads,
                              FailureDetector failureDetector) {
        if(isPipelineRoutedStoreEnabled) {
            Map<Integer, NonblockingStore> nonblockingStores = Maps.newHashMap();

            for(Map.Entry<Integer, Store<ByteArray, byte[]>> entry: nodeStores.entrySet()) {
                NonblockingStore nonblockingStore = null;

                if(entry.getValue() instanceof NonblockingStore)
                    nonblockingStore = (NonblockingStore) entry.getValue();
                else
                    nonblockingStore = new ThreadPoolBasedNonblockingStoreImpl(threadPool,
                                                                               entry.getValue());

                nonblockingStores.put(entry.getKey(), nonblockingStore);
            }

            return new PipelineRoutedStore(storeDefinition.getName(),
                                           nodeStores,
                                           nonblockingStores,
                                           cluster,
                                           storeDefinition,
                                           repairReads,
                                           routingTimeoutMs,
                                           failureDetector);
        } else {
            return new ThreadPoolRoutedStore(storeDefinition.getName(),
                                             nodeStores,
                                             cluster,
                                             storeDefinition,
                                             repairReads,
                                             threadPool,
                                             routingTimeoutMs,
                                             failureDetector,
                                             SystemTime.INSTANCE);
        }
    }

}