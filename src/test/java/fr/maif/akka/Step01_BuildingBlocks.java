package fr.maif.akka;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.testkit.javadsl.TestKit;
import io.vavr.API;
import io.vavr.collection.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;
import java.util.concurrent.CompletionStage;

import static akka.pattern.Patterns.pipe;
import static io.vavr.API.println;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static scala.compat.java8.FutureConverters.toScala;

@DisplayName("Construire des streams avec AKKA")
@TestMethodOrder(OrderAnnotation.class)
public class Step01_BuildingBlocks {

    private ActorSystem system = ActorSystem.create("BuildingBlocks");
    private ActorMaterializer mat = ActorMaterializer.create(system);

    @DisplayName("Un premier stream")
    @Test()
    @Order(1)
    public void monPremierStream() {
        // On va créer une source qui émet tous les éléments de 1 à 10.
        // NotUsed correspond à la valeur "materialisée" de cetter source, comme on ne s'en sert pas pour l'instant
        // on indique le type NotUsed (équivalent à Void)
        final Source<Integer, NotUsed> source = Source.range(1, 10);
        // Un sink qui affiche chaque valeur reçue
        final Sink<Integer, CompletionStage<Done>> sink = Sink.foreach(API::println);
        // L'expression source to sink défini un graphe
        final RunnableGraph<NotUsed> graph = source.to(sink);

        graph.run(mat); // le graphe ne fait rien tant qu'on appelle pas la méthode run.
    }

    @DisplayName("Transformer les éléments avec un Flow")
    @Test
    @Order(2)
    public void lesFlowTransformentLesElements() {

        final Source<Integer, NotUsed> source = Source.range(1, 10);

        final Flow<Integer, Integer, NotUsed> flow = Flow.fromFunction(x -> x + 1);

        final Sink<Integer, CompletionStage<Done>> sink = Sink.foreach(API::println);

        // une source connectée à un flow retourne une nouvelle source
        final Source<Integer, NotUsed> sourceWithFlow = source.via(flow);

        final Sink<Integer, NotUsed> flowWithSink = flow.to(sink);

        // equivalent
        //sourceWithFlow.to(sink).run(mat);
        source.to(flowWithSink).run(mat);
        //source.via(flow).to(sink).run(mat);
    }

    @DisplayName("Valeur nulle non autorisée")
    @Test
    @Order(3)
    public void nullsAreNotAllowed() {
        assertThrows(NullPointerException.class, () -> {
            final Source<Object, NotUsed> illegalSource = Source.single(null);
            illegalSource.to(Sink.foreach(API::println)).run(mat);
            // use Options instead
        });
    }

    @DisplayName("Différents types de Sources")
    @Test
    @Order(4)
    public void differentsTypesDeSources() {
        final Source<Integer, NotUsed> sourceFini = Source.single(1);
        final Source<Integer, NotUsed> autreSourceFini = Source.from(asList(1, 2, 3));
        final Source<Integer, NotUsed> sourceVide = Source.empty();
        // do not confuse an Akka stream with a "collection" Stream
        final Source<Integer, NotUsed> sourceInfini = Source.from(Stream.from(1));
        // On peut aussi créer une source à partir d'autres choses, ex: une future
        final Source<Integer, NotUsed> futureSource = Source.fromCompletionStage(completedFuture(42));
    }

    @DisplayName("Différents types de Sinks")
    @Test
    @Order(5)
    public void sinks() {
        final Sink<Object, CompletionStage<Done>> theMostBoringSink = Sink.ignore();
        final Sink<String, CompletionStage<Done>> foreachSink = Sink.foreach(API::println);
        // retrieves head and then closes the stream
        final Sink<Integer, CompletionStage<Integer>> headSink = Sink.head();
        // this sink is able to compute the sum of all the elements that are passed into it.
        final Sink<Integer, CompletionStage<Integer>> foldSink = Sink.fold(0, (a, b) -> a + b);
    }

    @DisplayName("Différents types de Flows")
    @Test
    @Order(6)
    public void flows() {
        final Source<Integer, NotUsed> source = Source.range(1, 10);

        // flows - usually mapped to collection operators
        final Flow<Integer, Integer, NotUsed> mapFlow = Flow.<Integer>create().map(x -> 2 * x);
        final Flow<Integer, Integer, NotUsed> takeFlow = Flow.<Integer>create().take(5);

        final Sink<Integer, CompletionStage<Done>> sink = Sink.foreach(API::println);

        // source -> flow -> flow -> ... -> sink
        source.via(mapFlow).via(takeFlow).to(sink).run(mat);

        println("==============");

        // syntactic sugars
        Source.range(1, 10).via(Flow.<Integer>create().map(x -> 2 * x));
        final Source<Integer, NotUsed> mapSource = Source.range(1, 10).map(x -> 2 * x);

        println("==============");

        // run streams directly
        mapSource.to(Sink.foreach(API::println)).run(mat);
        mapSource.runForeach(API::println, mat);
    }

    /**
     * Exercice: creer un stream qui prend le nom de personnes en entrée,
     *           puis conserver les 2 premiers noms ayant plus de 5 caractères.
     */
    @DisplayName("Exercice : ")
    @Test
    @Order(7)
    public void exercise() {
        final Source<String, NotUsed> nameSource = Source.from(
                asList("Alice", "Bob", "Charlie", "David", "Martin", "AkkaStreams"));

        final Flow<String, String, NotUsed> longNameFlow = Flow.<String>create().filter(s -> s.length() > 5);
        final Flow<String, String, NotUsed> limitFlow = Flow.<String>create().take(2);
        final Sink<String, CompletionStage<Done>> nameSink = Sink.foreach(API::println);

        //nameSource.via(longNameFlow).via(limitFlow).to(nameSink).run(mat);
        nameSource
                .filter(__ -> __.length() > 5)
                .take(2)
                .runForeach(API::println, mat);
    }

    @DisplayName("TU sur Stream")
    @Test
    @Order(8)
    public void pipeToTestProbe() {
        // acheminer jusqu'à la sonde
        final Source<List<Integer>, NotUsed> sourceUnderTest = Source.from(asList(1, 2, 3, 4)).grouped(2);

        final CompletionStage<List<List<Integer>>> future = sourceUnderTest.grouped(2).runWith(Sink.head(), mat);

        final TestKit probe = new TestKit(system);
        // With the Pipe pattern you can take a future and “pipe” the result of that future to another actor.
        pipe(toScala(future), system.dispatcher()).to(probe.getRef());

        probe.expectMsg(ofSeconds(3), asList(asList(1, 2), asList(3, 4)));
    }
}
