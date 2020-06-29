package demo;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Source;
import io.vavr.API;
import io.vavr.collection.List;

import static java.time.Duration.ZERO;
import static java.time.Duration.ofSeconds;

public class JavaDemo {

    public static void main(String[] args) {

        ActorSystem system = ActorSystem.create("AkkaStreams");
        Materializer materializer = ActorMaterializer.create(system);

        Source<String, NotUsed> hey = Source.repeat("Hey");

        Source<String, Cancellable> yo = Source.tick(ZERO, ofSeconds(1), "Yo");

        Source.zipWithN(
                l -> List.ofAll(l).mkString(" "),
                List.of(hey, yo).toJavaList())
                .scan("", (acc, elt) -> acc + " - " + elt)
                .runForeach(API::println, materializer);

    }
}