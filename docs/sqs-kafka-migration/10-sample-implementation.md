# 샘플 구현 청사진

다음 코드는 마이그레이션 기간 동안 SQS와 Kafka에 듀얼 라이트를 수행할 수 있도록 스프링 부트 서비스에 메시징 추상화를 도입하는 예시입니다.

```java
// src/main/java/com/example/messaging/MessageEnvelope.java
package com.example.messaging;

import java.time.Instant;
import java.util.Map;

public record MessageEnvelope<T>(
        String id,
        String eventType,
        Instant occurredAt,
        T payload,
        Map<String, String> headers
) {}
```

```java
// src/main/java/com/example/messaging/MessagePublisher.java
package com.example.messaging;

public interface MessagePublisher {
    void publish(MessageEnvelope<?> message);
}
```

```java
// src/main/java/com/example/messaging/SqsMessagePublisher.java
package com.example.messaging;

import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
class SqsMessagePublisher implements MessagePublisher {

    private final QueueMessagingTemplate template;

    SqsMessagePublisher(QueueMessagingTemplate template) {
        this.template = template;
    }

    @Override
    public void publish(MessageEnvelope<?> message) {
        template.convertAndSend("domain-events", message);
    }
}
```

```java
// src/main/java/com/example/messaging/KafkaMessagePublisher.java
package com.example.messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class KafkaMessagePublisher implements MessagePublisher {

    private final KafkaTemplate<String, Object> template;

    KafkaMessagePublisher(KafkaTemplate<String, Object> template) {
        this.template = template;
    }

    @Override
    public void publish(MessageEnvelope<?> message) {
        template.send("domain-events", message.id(), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        // adopt centralized error handling or retry policy
                        throw new RuntimeException("Kafka publish failed", ex);
                    }
                });
    }
}
```

```java
// src/main/java/com/example/messaging/DualWriteMessagePublisher.java
package com.example.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(name = "messaging.dual-write-enabled", havingValue = "true")
@Component
class DualWriteMessagePublisher implements MessagePublisher {

    private final MessagePublisher sqsPublisher;
    private final MessagePublisher kafkaPublisher;

    DualWriteMessagePublisher(
            SqsMessagePublisher sqsPublisher,
            KafkaMessagePublisher kafkaPublisher
    ) {
        this.sqsPublisher = sqsPublisher;
        this.kafkaPublisher = kafkaPublisher;
    }

    @Override
    public void publish(MessageEnvelope<?> message) {
        sqsPublisher.publish(message);
        kafkaPublisher.publish(message);
    }
}
```

```java
// src/main/java/com/example/messaging/MessagingPublisherFactory.java
package com.example.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MessagingPublisherFactory {

    @Bean
    @ConditionalOnProperty(name = "messaging.transport", havingValue = "sqs")
    MessagePublisher sqsOnlyPublisher(SqsMessagePublisher sqs) {
        return sqs;
    }

    @Bean
    @ConditionalOnProperty(name = "messaging.transport", havingValue = "kafka")
    MessagePublisher kafkaOnlyPublisher(KafkaMessagePublisher kafka) {
        return kafka;
    }

    @Bean
    @ConditionalOnMissingBean
    MessagePublisher defaultDualPublisher(DualWriteMessagePublisher dual) {
        return dual;
    }
}
```

```java
// src/main/java/com/example/messaging/KafkaConsumerConfig.java
package com.example.messaging;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.Map;

@Configuration
class KafkaConsumerConfig {

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, MessageEnvelope<?>> kafkaListenerContainerFactory() {
        var props = Map.<String, Object>of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092",
                ConsumerConfig.GROUP_ID_CONFIG, "domain-consumer",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "com.example.messaging.MessageEnvelopeDeserializer"
        );

        var factory = new ConcurrentKafkaListenerContainerFactory<String, MessageEnvelope<?>>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}
```

These snippets demonstrate:
- 비즈니스 로직과 전송 수단 선택을 분리하는 인터페이스 구성.
- 코드 재배포 없이 SQS 전용, Kafka 전용, 듀얼 라이트 모드를 제어하는 기능 플래그.
- SQS Visibility Timeout을 대체하는 명시적 Kafka 컨슈머 설정.

프로젝트 표준에 맞춰 패키지명, 토픽/큐 이름, 직렬화 방식을 조정하고 실제 서비스 구현 시 `@KafkaListener` 등 컨슈머 예제도 추가하세요.
