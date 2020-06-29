package jug.workshops.reactive.akka.basics;

import akka.japi.JavaPartialFunction;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import io.vavr.collection.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.*;
import scala.PartialFunction;

import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;

@DisplayName("PartialFunctions")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BasicsPart4PartialFunctionsSpecAnswers {

    @DisplayName("handle addition and multiplication")
    @Test
    @Order(1)
    public void partialFunctionCalculator() {

        JavaPartialFunction<Tuple3<Integer, Integer, String>, Integer> add = new JavaPartialFunction<>() {
            @Override
            public Integer apply(Tuple3<Integer, Integer, String> t3, boolean isCheck) {
                return Match(t3._3).option(Case($("+"), t3._1 + t3._2)).getOrElseThrow(() -> noMatch());
            }
        };

        JavaPartialFunction<Tuple3<Integer, Integer, String>, Integer> mult = new JavaPartialFunction<>() {
            @Override
            public Integer apply(Tuple3<Integer, Integer, String> t3, boolean isCheck) {
                return Match(t3._3).option(Case($("*"), t3._1 * t3._2)).getOrElseThrow(() -> noMatch());
            }
        };

        final PartialFunction<Tuple3<Integer, Integer, String>, Integer> calc = add.orElse(mult);

        Assertions.assertEquals(3,  calc.apply(Tuple.of(1, 2, "+")));
        Assertions.assertEquals(12, calc.apply(Tuple.of(6, 2, "*")));
        Assertions.assertEquals(18, calc.apply(Tuple.of(6, 3, "*")));
    }

    @DisplayName("preserve operations history")
    @Test
    @Order(2)
    public void calculatorWithState() {

        CalcWithState encapsulatedState = new CalcWithState();
        Sender testProbe = new Sender();

        encapsulatedState.receive(new ObjectWithState.Add(2));
        encapsulatedState.receive(new ObjectWithState.Add(3));
        encapsulatedState.receive(new ObjectWithState.PrintState(testProbe));
        encapsulatedState.receive(new ObjectWithState.Mult(6));
        encapsulatedState.receive(new ObjectWithState.PrintState(testProbe));

        org.assertj.core.api.Assertions.assertThat(testProbe.getStatesInternal()).containsExactly(5, 30);
    }

    interface ObjectWithState {
        @AllArgsConstructor
        class Add implements ObjectWithState {
            private final Integer v;
        }
        @AllArgsConstructor
        class Mult implements ObjectWithState {
            private final Integer v;
        }
        @AllArgsConstructor
        class PrintState implements ObjectWithState {
            private final Sender sender;
        }
    }


    @Getter
    class Sender {
        private List<Integer> statesInternal;

        public Sender() {
            this.statesInternal = List.empty();
        }

        private List<Integer> addState(Integer newState) {
            statesInternal = this.statesInternal.append(newState);
            return statesInternal;
        }
    }

    class CalcWithState {

        private Integer state;

        public CalcWithState() {
            this.state = 0;
        }

        public Object receive(Object e) {
            return Match(e).of(
                    Case($(instanceOf(ObjectWithState.Add.class)), n -> state = state + n.v),
                    Case($(instanceOf(ObjectWithState.Mult.class)), n -> state = state * n.v),
                    Case($(instanceOf(ObjectWithState.PrintState.class)), ps -> ps.sender.addState(this.state)));
        }
    }
}
