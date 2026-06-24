package com.example.lms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.cache.type=none",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.datasource.url=jdbc:h2:mem:lms-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false"
})
class LmsApplicationTests {

    @Test
    void contextLoads() {
    }

}
