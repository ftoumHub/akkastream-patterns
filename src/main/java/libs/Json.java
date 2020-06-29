package libs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.vavr.jackson.datatype.VavrModule;

public class Json {

    public static ObjectMapper defaultObjectMapper = newDefaultMapper();

    private static ObjectMapper newDefaultMapper() {

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new VavrModule());
        //mapper.registerModule(new Jdk8Module());
        //mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        return mapper;
    }
}
