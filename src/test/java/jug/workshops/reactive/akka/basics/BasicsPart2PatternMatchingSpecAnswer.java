package jug.workshops.reactive.akka.basics;

import io.vavr.Tuple;
import io.vavr.Tuple3;
import jug.workshops.reactive.akka.basics.BasicsPart2PatternMatchingSpecAnswer.Expression.Add;
import jug.workshops.reactive.akka.basics.BasicsPart2PatternMatchingSpecAnswer.Expression.Mult;
import jug.workshops.reactive.akka.basics.BasicsPart2PatternMatchingSpecAnswer.Expression.Number;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.*;

import static io.vavr.API.*;
import static io.vavr.Patterns.*;
import static io.vavr.Predicates.instanceOf;
import static io.vavr.Tuple.of;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("PatternMatchingExercise")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BasicsPart2PatternMatchingSpecAnswer {

    @DisplayName("properly match message (Int,Int,String)")
    @Test
    @Order(1)
    public void matchMessage() {
        // avec switch
        assertEquals(10, primaryExercise(8, 2, "+"));
        assertEquals(4,  primaryExercise(3, 1, "+"));
        assertEquals(6,  primaryExercise(8, 2, "-"));
        assertEquals(2,  primaryExercise(3, 1, "-"));
        assertEquals(16, primaryExercise(8, 2, "*"));
        assertEquals(3,  primaryExercise(3, 1, "*"));

        // avec match
        assertEquals(10, primaryExercise(of(8, 2, "+")));
        assertEquals(6,  primaryExercise(of(8, 2, "-")));
        assertEquals(16, primaryExercise(of(8, 2, "*")));
    }

    @DisplayName("Properly match case classes")
    @Test
    @Order(2)
    public void matchCaseClass() {
        assertEquals(additionalExercise(new Number(7)), 7);
        assertEquals(additionalExercise(new Number(9)), 9);
        assertEquals(additionalExercise(new Add(new Number(7), new Number(2))), 9);
        assertEquals(additionalExercise(new Add(new Number(7), new Number(5))), 12);
        assertEquals(additionalExercise(new Mult(new Number(7), new Number(2))), 14);
        assertEquals(additionalExercise(new Mult(new Number(7), new Number(3))), 21);
    }

    static Integer primaryExercise(Integer t1, Integer t2, String sign) {
        Integer result = 0;
        switch (sign) {
            case "+": result = t1 + t2;
                break;
            case "-": result = t1 - t2;
                break;
            case "*": result = t1 * t2;
                break;
        }
        return result;
    }

    static Integer primaryExercise(Tuple3<Integer, Integer, String> t3) {
        return Match(t3).of(
                Case($Tuple3($(), $(), $("+")), () -> t3._1 + t3._2),
                Case($Tuple3($(), $(), $("-")), () -> t3._1 - t3._2),
                Case($Tuple3($(), $(), $("*")), () -> t3._1 * t3._2));
    }

    interface Expression {
        @AllArgsConstructor
        class Number implements Expression {
            private final Integer v;
        }
        @AllArgsConstructor
        class Add implements Expression {
            private final Number n1;
            private final Number n2;
        }
        @AllArgsConstructor
        class Mult implements Expression {
            private final Number n1;
            private final Number n2;
        }
    }

    static Integer additionalExercise(Expression e) {
        return Match(e).of(
                Case($(instanceOf(Number.class)), n1 -> n1.v),
                Case($(instanceOf(Add.class)), a -> a.n1.v + a.n2.v),
                Case($(instanceOf(Mult.class)), m -> m.n1.v * m.n2.v));
    }

}
