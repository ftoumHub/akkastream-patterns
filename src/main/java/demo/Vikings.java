package demo;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.japi.JavaPartialFunction;
import akka.stream.ActorMaterializer;
import akka.stream.FlowShape;
import akka.stream.UniformFanInShape;
import akka.stream.UniformFanOutShape;
import akka.stream.javadsl.Balance;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Framing;
import akka.stream.javadsl.GraphDSL;
import akka.stream.javadsl.Merge;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vavr.API;
import io.vavr.collection.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import play.libs.ws.WSResponse;
import play.libs.ws.ahc.AhcWSClient;
import play.libs.ws.ahc.StandaloneAhcWSClient;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import static akka.stream.javadsl.FramingTruncation.ALLOW;
import static com.typesafe.config.ConfigFactory.load;
import static io.vavr.API.println;
import static java.lang.ClassLoader.getSystemResource;
import static play.libs.ws.ahc.AhcWSClientConfigFactory.forConfig;

public class Vikings {

    private static final Logger logger = LoggerFactory.getLogger(Vikings.class);

    private static ActorSystem system = ActorSystem.create("vikings-controller");
    private static ActorMaterializer materializer = ActorMaterializer.create(system);

    private static final AhcWSClient wsClient = new AhcWSClient(
            StandaloneAhcWSClient.create(forConfig(load(), system.getClass().getClassLoader()), materializer), materializer);

    public static void main(String[] args) throws URISyntaxException {

        Path csvPath = Paths.get(getSystemResource("vikings.csv").toURI());

        Function<List<String>, ObjectNode> serializeNamePlace = namePlaceList -> {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode objectNode = objectMapper.createObjectNode();
            objectNode.put("name", namePlaceList.head());
            objectNode.put("place", namePlaceList.last());

            return objectNode;
        };

        final JavaPartialFunction<List<String>, ObjectNode> listObjectNodePartialFunction = new JavaPartialFunction<>() {
            @Override
            public ObjectNode apply(List<String> x, boolean isCheck) {
                return serializeNamePlace.apply(x);
            }
        };

        final List<String> servers = List.of("localhost:9200", "localhost:9201");
        println(index());

        Function<List<ObjectNode>, String> getBulk = bulk ->
            bulk.flatMap(j -> List.of(index(), j)).map(Json::stringify).mkString("", "\n", "\n");

        FileIO.fromPath(csvPath)
                .via(Framing.delimiter(ByteString.fromString("\n"), 1000, ALLOW))
                .map(__ -> __.utf8String())
                .drop(1)
                .map(__ -> List.of(__.split(";")))
                .collect(listObjectNodePartialFunction)
                .grouped(5) // Attention ici c'est une java.util.List qui est retournée
                .map(List::ofAll)
                .via(loadBalancing(servers, server -> {
                    logger.debug(server + " for flow");
                    final Flow<List<ObjectNode>, WSResponse, NotUsed> listWSResponseNotUsedFlow = Flow.<List<ObjectNode>>create().mapAsync(1, bulk -> {
                        final String strBulk = getBulk.apply(bulk);
                        logger.debug(server + " -> " + strBulk);
                        return wsClient.url(String.format("http://%s/_bulk", server))
                                .addHeader("Content-Type", "application/x-ndjson")
                                .post(strBulk);
                    });
                    return listWSResponseNotUsedFlow;
                }))
                .runForeach(API::println, materializer)
                .thenAccept(d -> {
                    logger.info("Lecture terminée");
                    system.terminate();
                });
    }

    private static ObjectNode index() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("_index","vikings");
        objectNode.put("_type", "vikings");
        ObjectNode objectRoot = objectMapper.createObjectNode();
        objectRoot.set("index", objectNode);

        return objectRoot;
    }

    private static <In, Out> Flow<In, Out, NotUsed> loadBalancing(List<String> servers, Function<String, Flow<In, Out, NotUsed>> flow) {
        return Flow.fromGraph(
                GraphDSL.create(
                        b -> {
                            final int paralellism = servers.size();
                            final UniformFanInShape<Out, Out> merge = b.add(Merge.create(paralellism));
                            final UniformFanOutShape<In, In> balance = b.add(Balance.create(paralellism));

                            servers.zipWithIndex().forEach(t -> {
                                logger.info("Server {} {}", t._1, t._2);
                                b.from(balance.out(t._2))
                                        .via(b.add(flow.apply(t._1).async()))
                                        .toInlet(merge.in(t._2));
                            });

                            return FlowShape.of(balance.in(), merge.out());
                        }));
    }
}
