package org.tudo.sse.multithreading;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.PostStop;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.tudo.sse.model.ArtifactIdent;
import org.tudo.sse.model.ArtifactResolutionContext;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is spawned in multiple threads
 * allowing for faster resolution of large quantity of artifacts.
 */
public class ResolverActor extends AbstractBehavior<WorkItem> {

    private static final AtomicInteger idCounter = new AtomicInteger(0);

    private final int id =  idCounter.getAndIncrement();
    private final ActorRef<WorkItem> queueActor;

    static Behavior<WorkItem> create(ActorRef<WorkItem> queueActor) {
        return Behaviors.setup(ctx -> new ResolverActor(ctx, queueActor));
    }

    private ResolverActor(ActorContext<WorkItem> ctx, ActorRef<WorkItem> queueActor) {
        super(ctx);

        this.queueActor = queueActor;

        ctx.getLog().info("Created resolver actor #{}", id);
    }

    private Behavior<WorkItem> onPostStop(){
        getContext().getLog().info("Stopped resolver actor #{}", id);
        return this;
    }

    @Override
    public Receive<WorkItem> createReceive(){
        return newReceiveBuilder()
                .onMessage(ProcessIdentifierMessage.class, message -> {
                    final ArtifactIdent identifier = message.getIdentifier();
                    final ArtifactResolutionContext ctx = message.getArtifactResolutionContext();

                    message.getInstance().callResolver(identifier, ctx);
                    message.getInstance().analyzeArtifact(ctx.getArtifact(identifier));

                    final var queueResponse = new WorkItemFinishedMessage(this.getContext().getSelf());

                    queueActor.tell(queueResponse);

                    return Behaviors.same();
                })
                .onMessage(ProcessLibraryMessage.class, message -> {
                    message.getProcessEntryCallback().get();

                    final var queueResponse = new WorkItemFinishedMessage(this.getContext().getSelf());

                    queueActor.tell(queueResponse);

                    return Behaviors.same();
                })
                .onMessage(WorkloadIsFinalMessage.class, msg -> Behaviors.stopped())
                .onSignal(PostStop.class, s -> onPostStop())
                .build();
    }

}
