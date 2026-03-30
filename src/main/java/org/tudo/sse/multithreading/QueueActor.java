package org.tudo.sse.multithreading;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.PostStop;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class represents the processing queue that resolves jobs and sends them to different threads.
 * The size of the jobs and threads are determined by the configuration set in CliInformation.
 */
public class QueueActor extends AbstractBehavior<WorkItem> {

    private final int maxNumberOfResolvers;
    private final AtomicInteger currentNumberOfResolvers;
    private final Queue<WorkItem> jobQueue;

    private boolean indexFinished = false;

    private final AtomicLong workItemsCompleted = new AtomicLong(0L);
    private final long progressSaveInterval;
    private long lastProgressSaved = 0L;
    private final Path progressOutputFilePath;


    /**
     * Creates a new queue actor behavior using the given configuration values
     * @param maxNumberOfResolvers Number of ResolverActor instances that will process jobs
     * @param initialWorkItemPosition The number of work items that have been skipped due to progress restore or skip
     * @param progressSaveInterval The number of completed work items after which to save progress
     * @param progressOutputFilePath The file path to which to write progress to
     * @return A new QueueActor behavior
     */
    public static Behavior<WorkItem> create(int maxNumberOfResolvers,
                                            long initialWorkItemPosition,
                                            long progressSaveInterval,
                                            Path progressOutputFilePath) {
        return Behaviors.setup(ctx ->
            new QueueActor(ctx, maxNumberOfResolvers, initialWorkItemPosition, progressSaveInterval, progressOutputFilePath)
        );
    }

    /**
     * Creates a new processing queue with the given number of actors and the given actor system.
     * @param ctx This actor's context
     * @param maxNumberOfResolvers Number of ResolverActor instances that will process jobs
     * @param initialWorkItemPosition The number of work items that have been skipped due to progress restore or skip
     * @param progressSaveInterval The number of completed work items after which to save progress
     * @param progressOutputFilePath The file path to which to write progress to
     */
    public QueueActor(ActorContext<WorkItem> ctx,
                      int maxNumberOfResolvers,
                      long initialWorkItemPosition,
                      long progressSaveInterval,
                      Path progressOutputFilePath) {
        super(ctx);

        this.maxNumberOfResolvers = maxNumberOfResolvers;
        this.currentNumberOfResolvers = new AtomicInteger(0);
        this.jobQueue = new LinkedList<>();

        this.progressSaveInterval = progressSaveInterval;
        this.progressOutputFilePath = progressOutputFilePath;
        this.workItemsCompleted.set(initialWorkItemPosition);

        ctx.getLog().info("Created queue actor");
    }

    private Behavior<WorkItem> onPostStop() {
        getContext().getLog().info("Stopped QueueActor");
        return this;
    }

    @Override
    public Receive<WorkItem> createReceive(){
        return newReceiveBuilder()
                .onMessage(ProcessIdentifierMessage.class, msg -> {
                    forwardToResolvers(msg);
                    return Behaviors.same();
                })
                .onMessage(ProcessLibraryMessage.class, msg -> {
                    forwardToResolvers(msg);
                    return Behaviors.same();
                })
                .onMessage(WorkItemFinishedMessage.class, msg -> {
                    // Track completion of work items
                    workItemsCompleted.incrementAndGet();
                    // Write progress file if needed
                    writeProgressIfNeeded();

                    final ActorRef<WorkItem> sender = msg.getSender();

                    synchronized (jobQueue) {
                        if(!jobQueue.isEmpty()){
                            WorkItem workItem = jobQueue.poll();
                            sender.tell(workItem);
                            if(jobQueue.size() % 10 == 0)
                                getContext().getLog().trace("Distributed a job, queue size {}", jobQueue.size());
                        } else {
                            synchronized (currentNumberOfResolvers){
                                if(indexFinished && currentNumberOfResolvers.get() == 1){
                                    getContext().getLog().trace("Shutting down queue actor...");
                                    return Behaviors.stopped();
                                } else {
                                    getContext().getLog().trace("Stopping a ResolverActor ...");
                                    sender.tell(WorkloadIsFinalMessage.getInstance());
                                    currentNumberOfResolvers.decrementAndGet();
                                }
                            }
                        }
                    }

                    return Behaviors.same();
                })
                .onMessage(WorkloadIsFinalMessage.class, msg -> {
                    this.indexFinished = true;
                    if(currentNumberOfResolvers.get() == 0)
                        return Behaviors.stopped();
                    else
                        return Behaviors.same();

                })
                .onSignal(PostStop.class, s -> onPostStop())
                .build();
    }

    private void forwardToResolvers(WorkItem message){
        synchronized (currentNumberOfResolvers){
            if(currentNumberOfResolvers.get() < maxNumberOfResolvers) {
                final ActorContext<WorkItem> ctx = getContext();
                final String name = "resolver-" + currentNumberOfResolvers.get();
                ActorRef<WorkItem> newResolver = ctx.spawn(ResolverActor.create(ctx.getSelf()), name);
                currentNumberOfResolvers.incrementAndGet();
                newResolver.tell(message);
            } else {
                jobQueue.add(message);
            }
        }
    }

    private void writeProgressIfNeeded(){
        final boolean needsWrite = (this.workItemsCompleted.get() - this.lastProgressSaved) > this.progressSaveInterval;

        if(needsWrite){
            synchronized (this.progressOutputFilePath){
                // Double-Check for efficient synchronization
                final boolean doesNeedWrite = (this.workItemsCompleted.get() - this.lastProgressSaved) > this.progressSaveInterval;
                if(doesNeedWrite){
                    try(BufferedWriter writer = new BufferedWriter(new FileWriter(this.progressOutputFilePath.toFile()))){
                        writer.write(String.valueOf(this.workItemsCompleted));
                    } catch(IOException ignored){}
                    this.lastProgressSaved = this.workItemsCompleted.get();
                }
            }
        }
    }

}
