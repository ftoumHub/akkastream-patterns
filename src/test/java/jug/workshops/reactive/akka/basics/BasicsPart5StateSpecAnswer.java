package jug.workshops.reactive.akka.basics;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.time.Duration;

@DisplayName("tateSpecAnswer")
@TestMethodOrder(OrderAnnotation.class)
public class BasicsPart5StateSpecAnswer {

    private static ActorSystem system;

    @BeforeAll
    public static void setUp() {
        system = ActorSystem.create("SimpleActor");
    }

    @AfterAll
    public static void tearDown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @DisplayName("remove duplicates")
    @Test
    @Order(1)
    public void removeDuplicates() {

        final TestKit probe = new TestKit(system);

        final ActorRef deduplicator = system.actorOf(Deduplicator.props(probe.getRef()));

        deduplicator.tell("MSG1", deduplicator);
        deduplicator.tell("MSG1", deduplicator);
        deduplicator.tell("MSG2", deduplicator);
        deduplicator.tell("MSG2", deduplicator);
        deduplicator.tell("MSG2", deduplicator);
        deduplicator.tell("MSG3", deduplicator);

        probe.expectMsg("MSG1");
        probe.expectMsg("MSG2");
        probe.expectMsg("MSG3");
        probe.expectNoMessage(Duration.ofMillis(500));
    }
    
    public static class Deduplicator extends AbstractActor {

        private final ActorRef next;

        static public Props props(ActorRef next) {
            return Props.create(Deduplicator.class, () -> new Deduplicator(next));
        }

        private Deduplicator(ActorRef next) {
            this.next = next;
        }

        Set<String> history = HashSet.empty();

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(String.class, msg -> {
                        if (!history.contains(msg)) {
                            history = history.add(msg);
                            next.tell(msg, self());
                        }
                    })
                    .build();
        }
    }
    
    public static class WordProcessor extends AbstractActor {

        private final ActorRef next;

        static public Props props(ActorRef next) {
            return Props.create(WordProcessor.class, () -> new WordProcessor(next));
        }

        private WordProcessor(ActorRef next) {
            this.next = next;
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(String.class, word -> next.tell(word.toUpperCase(), self()))
                    .build();
        }
    }
}
