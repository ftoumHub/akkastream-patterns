package jug.workshops.reactive.akka.basics;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import org.junit.jupiter.api.*;

import java.util.List;

import static io.vavr.API.println;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DisplayName("ActorThreads")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BasicsPart3ThreadsAndActorsSpecAnswer {

    private static ActorSystem system;
    private static Long mainThreadId = Thread.currentThread().getId();
    private static String mainThreadName = Thread.currentThread().getName();

    @BeforeAll
    public static void setUp() {
        system = ActorSystem.create("actorSystem");
    }

    @AfterAll
    public static void tearDown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @DisplayName("executing logic in different Thread")
    @Test
    @Order(1)
    public void logicInDifferentThread() {
        TestKit probe = new TestKit(system);

        ActorRef exerciseActor = system.actorOf(Props.create(ExerciseActor.class), "exerciseActorTest1");

        exerciseActor.tell(new ThreadId(), probe.getRef());
        final Long actorThreadId = probe.expectMsgClass(Long.class);

        exerciseActor.tell(new ThreadName(), probe.getRef());
        final String actorThreadName = probe.expectMsgClass(String.class);

        println(String.format("actor is working in [id : %s, name: %s]", actorThreadId, actorThreadName));
        println(String.format("main thread is [id : %s, name: %s]", mainThreadId, mainThreadName));

        assertNotEquals(actorThreadId, mainThreadId);
        assertNotEquals(actorThreadName, mainThreadName);
    }

    @DisplayName("handle list of requests")
    @Test
    @Order(2)
    public void handleListOfRequest() {
        TestKit probe = new TestKit(system);

        ActorRef exerciseActor = system.actorOf(Props.create(ExerciseActor.class), "exerciseActorTest2");

        exerciseActor.tell(asList(new ThreadId(), new ThreadName()), probe.getRef());
        final Long actorThreadId = probe.expectMsgClass(Long.class);
        final String actorThreadName = probe.expectMsgClass(String.class);

        println(String.format("actor is working in [id : %s, name: %s]", actorThreadId, actorThreadName));
        println(String.format("main thread is [id : %s, name: %s]", mainThreadId, mainThreadName));

        assertNotEquals(actorThreadId, mainThreadId);
        assertNotEquals(actorThreadName, mainThreadName);
    }

    static class ThreadId {}
    static class ThreadName {}

    public static class ExerciseActor extends AbstractActor {
        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(ThreadId.class, m -> sender().tell(currentThread().getId(), self()))
                    .match(ThreadName.class, m -> sender().tell(currentThread().getName(), self()))
                    .match(List.class, l -> l.forEach(e -> self().forward(e, context())))
                    .build();
        }
    }
}
