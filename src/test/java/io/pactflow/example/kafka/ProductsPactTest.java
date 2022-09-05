package io.pactflow.example.kafka;

import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Interaction;
import au.com.dius.pact.core.model.V4Pact;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(PactConsumerTestExt.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@PactTestFor(providerName = "pactflow-example-provider-java-kafka", providerType = ProviderType.ASYNCH)
public class ProductsPactTest {
    @Autowired
    ProductEventListener listener;

    @Pact(consumer = "pactflow-example-consumer-java-kafka")
    V4Pact createPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringType("name", "product name");
        body.stringType("type", "product series");
        body.stringType("id", "5cc989d0-d800-434c-b4bb-b1268499e850");
        body.stringMatcher("version", "v[a-zA-z0-9]+", "v1");
        body.stringMatcher("event", "^(CREATED|UPDATED|DELETED)$", "CREATED");

        return builder.expectsToReceive("a product created event").withMetadata(md -> {
            md.add("Content-Type", "application/json");
            md.add("kafka_topic", "products");
            md.add("test", "test");
        }).withContent(body).toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createPact", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
    void test(V4Interaction.AsynchronousMessage message) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        System.out.println("Message received -> " + message.getContents().getContents().valueAsString());
        Product product = mapper.readValue(message.getContents().getContents().valueAsString(), Product.class);

        assertDoesNotThrow(() -> {
            listener.listen(product);
        });
    }
}