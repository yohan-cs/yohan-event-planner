package com.yohan.event_planner;

import com.yohan.event_planner.util.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@Import(TestConfig.class)
class EventPlannerApplicationTests {

	@Test
	void contextLoads() {
	}

}
