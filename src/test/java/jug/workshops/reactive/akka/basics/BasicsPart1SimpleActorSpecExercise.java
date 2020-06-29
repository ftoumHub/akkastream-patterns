package jug.workshops.reactive.akka.basics;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.junit.jupiter.api.*;

import static io.vavr.API.println;

@DisplayName("SimpleActorTest")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BasicsPart1SimpleActorSpecExercise {

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

    @DisplayName("renvoi true si 'i' est Pair et false si 'i' est Impair")
    @Test
    @Order(1)
    public void firstActorUnitTest() {
        TestKit probe = new TestKit(system);

        // We don't want actor to exist outside an ActorSystem context that's why we are passing
        // special Props to a factory method so an actor isn't created outside our control.
        ActorRef actorUnderTesting = system.actorOf(Props.create(SimpleActorForTestingAnswer.class));

        Integer even = 20;
        Integer odd = 21;

        actorUnderTesting.tell(even, probe.getRef());
        probe.expectMsg(true);

        actorUnderTesting.tell(odd, probe.getRef());
        probe.expectMsg(false);
    }

    @DisplayName("renvoi la somme d'un tuple")
    @Test
    @Order(2)
    public void returnSumOfTuple() {
        TestKit probe = new TestKit(system);

        ActorRef actorUnderTesting = system.actorOf(Props.create(SimpleActorForTestingAnswer.class));

        Tuple2 t1 = Tuple.of(3, 7);
        Tuple2 t2 = Tuple.of(-2, 5);

        actorUnderTesting.tell(t1, probe.getRef());
        probe.expectMsg(10);

        actorUnderTesting.tell(t2, probe.getRef());
        probe.expectMsg(3);
    }

    @DisplayName("multiplier les elements du tuple reçu : (i1,i2) => i1*i2")
    @Test
    @Order(3)
    public void multiplyElementsOfReceivedTuple() {
        TestKit probe = new TestKit(system);

        ActorRef actorUnderTesting = system.actorOf(Props.create(MultiplyingActor.class));

        Tuple2 t1 = Tuple.of(3, 5);
        Tuple2 t2 = Tuple.of(2, 3);

        actorUnderTesting.tell(t1, probe.getRef());
        probe.expectMsg(15);

        actorUnderTesting.tell(t2, probe.getRef());
        probe.expectMsg(6);
    }

    /**
     * Un acteur simple
     */
    public static class SimpleActorForTestingAnswer extends AbstractActor {

        /**
         * On défini ici les messages que cet acteur peut gérer,
         * ainsi que l'implémentation du traitement du message.
         * On utilise le pattern matching pour gérer les différents messages.
         *
         * On utilise sender() pour envoyer une réponse à l'appelant via sa référence.
         */
        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(Integer.class, i -> sender().tell((i % 2 == 0), self()))
                    .match(Tuple2.class, t -> sender().tell(((Integer)t._1 + (Integer)t._2), self()))
                    .matchAny(m -> println("in actor : received unknown message : [value="+m+", type="+m.getClass()+" ]"))
                    .build();
        }
    }

    public static class MultiplyingActor extends AbstractActor {
        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(Tuple2.class, t -> sender().tell(((Integer)t._1 * (Integer)t._2), self()))
                    .build();
        }
    }
}
