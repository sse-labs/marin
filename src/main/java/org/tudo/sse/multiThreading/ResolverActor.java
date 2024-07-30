package org.tudo.sse.multiThreading;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import org.tudo.sse.ArtifactFactory;

/**
 * This class is spawned in multiple threads allowing for faster resolution of large quantity of artifacts.
 */
public class ResolverActor extends AbstractActor {

    public static Props props() {
        return Props.create(ResolverActor.class, ResolverActor::new);
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(IdentPlusMCA.class, message -> {
                    message.getInstance().callResolver(message.getIdentifier());
                    message.getInstance().analyzeArtifact(ArtifactFactory.getArtifact(message.getIdentifier()));
                    getSender().tell("Finished", getSelf());
                }).build();
    }

}
