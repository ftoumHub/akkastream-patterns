package code;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;

import static java.time.Duration.ofSeconds;

public class BufferProblems {

    private static final ActorSystem system = ActorSystem.create("BufferProblems");
    private static final ActorMaterializer mat = ActorMaterializer.create(system);
    private final static LoggingAdapter log = Logging.getLogger(system, BufferProblems.class);

    static class Tick{
        @Override
        public String toString() {
            return "Tick()";
        }
    }

    public static void main(String[] args) {

        final Source<Tick, Cancellable> fastSource = Source.tick(ofSeconds(1), ofSeconds(1), new Tick());
        final Source<Tick, Cancellable> slowSource = Source.tick(ofSeconds(3), ofSeconds(3), new Tick());

        final Flow<Integer, Pair<Integer, Tick>, NotUsed> asyncZip = Flow.<Integer>create().zip(slowSource).async();

        fastSource
                .conflateWithSeed(__ -> 1, (count, __) -> count + 1)
                .log("Before AsyncZip")
                .via(asyncZip)
                .take(10)
                .log("After AsyncZip")
                .runForeach(p -> log.debug("Received: {}", p.first()), mat);
    }

}
