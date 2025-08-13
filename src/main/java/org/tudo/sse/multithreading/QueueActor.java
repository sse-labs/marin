package org.tudo.sse.multithreading;

import akka.actor.*;
import akka.japi.pf.ReceiveBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class represents the processing queue that resolves jobs and sends them to different threads.
 * The size of the jobs and threads are determined by the configuration set in CliInformation.
 * @see org.tudo.sse.CliInformation
 */
public class QueueActor extends AbstractActor {

    private final int numResolverActors;
    private final AtomicInteger curNumResolvers;
    private final Queue<ProcessIdentifierMessage> jobQueue;
    private boolean indexFinished = false;
    private final ActorSystem system;

    private static final Logger log = LogManager.getLogger(QueueActor.class);

    /**
     * Creates a new processing queue with the given number of actors and the given actor system.
     * @param numResolverActors Number of ResolverActor instances that will process jobs
     * @param system The underlying ActorSystem
     */
    public QueueActor(int numResolverActors, ActorSystem system) {
        this.numResolverActors = numResolverActors;
        this.system = system;
        this.curNumResolvers = new AtomicInteger(0);
        this.jobQueue = new LinkedList<>();
    }

    /**
     * Creates the properties needed to initialize an actor instance of this queue
     * @param numResolverActors The number of ResolverActor instances that shall be used to process jobs
     * @param system The underlying ActorSystem
     * @return The AKKA actor properties
     */
    public static Props props(int numResolverActors, ActorSystem system) {
        return Props.create(QueueActor.class, () -> new QueueActor(numResolverActors, system));
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(ProcessIdentifierMessage.class, message -> {
                    synchronized (curNumResolvers){
                        if(curNumResolvers.get() < numResolverActors) {
                            ActorRef processor = getContext().actorOf(ResolverActor.props());
                            processor.tell(message, getSelf());
                            log.info("New resolver created");
                            curNumResolvers.incrementAndGet();
                        } else {
                            jobQueue.add(message);
                        }
                    }

                })
                .match(String.class, message -> {
                    synchronized (jobQueue){
                        if(!jobQueue.isEmpty()) {
                            getSender().tell(jobQueue.peek(), getSelf());
                            jobQueue.remove();
                            if(jobQueue.size() % 10 == 0) log.trace("Distributed a job, queue size " + jobQueue.size());
                        } else {
                            synchronized(curNumResolvers) {
                                if(indexFinished && curNumResolvers.get() == 1) {
                                    log.trace("Shutting down system");
                                    system.terminate();
                                } else {
                                    log.trace("Killing a worker thread");
                                    getSender().tell(PoisonPill.getInstance(), getSelf());
                                    curNumResolvers.decrementAndGet();
                                }
                            }
                        }
                    }

                })
                .match(IndexProcessingMessage.class, indexProcessingMessage -> {
                    indexFinished = true;
                    if(curNumResolvers.get() == 0) {
                        system.terminate();
                    }
                })
                .build();
    }

}
