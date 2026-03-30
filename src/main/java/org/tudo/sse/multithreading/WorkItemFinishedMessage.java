package org.tudo.sse.multithreading;

import org.apache.pekko.actor.typed.ActorRef;

/**
 * A message that is used in multithreaded mode to let the queue manager keep track of the number of completed
 * work items (libraries or artifacts processed).
 */
public class WorkItemFinishedMessage implements WorkItem {

    private final ActorRef<WorkItem> sender;

    /**
     * Creates a new WorkItemFinishedMessage for the given sender.
     * @param sender Actor that send this message
     */
    public WorkItemFinishedMessage(ActorRef<WorkItem> sender) {
        this.sender = sender;
    }

    /**
     * Gets the actor ref that sent this message.
     * @return Sender reference
     */
    public ActorRef<WorkItem> getSender() {
        return this.sender;
    }
}
