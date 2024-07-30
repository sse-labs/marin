package org.tudo.sse.multiThreading;

import akka.actor.*;
import akka.japi.pf.ReceiveBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.Queue;

/**
 * This class is a queue in handling resolution jobs to send to different threads.
 * The size of the jobs and threads are determined by the configuration set in CliInformation.
 * @see org.tudo.sse.CliInformation
 */
public class QueueActor extends AbstractActor{

    private final int numResolverActors;
    private int curNumResolvers;
    private final Queue<IdentPlusMCA> jobQueue;
    private boolean indexFinished = false;
    private final ActorSystem system;

    public static final Logger log = LogManager.getLogger(QueueActor.class);

    public QueueActor(int numResolverActors, ActorSystem system) {
        this.numResolverActors = numResolverActors;
        this.system = system;
        this.curNumResolvers = 0;
        this.jobQueue = new LinkedList<>();
    }

    public static Props props(int numResolverActors, ActorSystem system) {
        return Props.create(QueueActor.class, () -> new QueueActor(numResolverActors, system));
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(IdentPlusMCA.class, message -> {
                    if(curNumResolvers != numResolverActors) {
                        ActorRef processor = getContext().actorOf(ResolverActor.props());
                        processor.tell(message, getSelf());
                        log.info("New resolver created");
                        curNumResolvers++;
                    } else {
                        log.info("Added to queue");
                        jobQueue.add(message);
                    }
                })
                .match(String.class, message -> {
                    if(!jobQueue.isEmpty()) {
                        getSender().tell(jobQueue.peek(), getSelf());
                        jobQueue.remove();
                    } else {
                        if(indexFinished && curNumResolvers == 1) {
                            system.terminate();
                        } else {
                            getSender().tell(PoisonPill.getInstance(), getSelf());
                            curNumResolvers--;
                        }
                    }
                })
                .match(IndexProcessingMessage.class, indexProcessingMessage -> {
                    indexFinished = true;
                    if(curNumResolvers == 0) {
                        system.terminate();
                    }
                })
                .build();
    }

}
