package org.tudo.sse.multithreading;

/**
 * This class is a marker message sent to the queue actor. It signals that no more work items will be scheduled in the
 * current analysis run.
 */
public class WorkloadIsFinalMessage implements WorkItem {

    private static WorkloadIsFinalMessage _instance;

    private WorkloadIsFinalMessage() {}

    /**
     * Retrieves the singleton instance of this message.
     * @return The singleton instance.
     */
    public static WorkloadIsFinalMessage getInstance(){
        if(_instance == null) _instance = new WorkloadIsFinalMessage();
        return _instance;
    }
}
