package com.yohan.event_planner.jobs;

import com.yohan.event_planner.repository.UserRepository;
import com.yohan.event_planner.domain.User;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

@Component
public class UserCleanupJob {

    private static final Logger logger = LoggerFactory.getLogger(UserCleanupJob.class);
    private final UserRepository userRepository;

    public UserCleanupJob(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "0 0 3 * * *") // runs every day at 3am UTC
    @Transactional
    public void deleteExpiredUsers() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        List<User> toDelete = userRepository.findAllByIsPendingDeletionTrueAndScheduledDeletionDateBefore(now);

        if (!toDelete.isEmpty()) {
            logger.info("Deleting {} expired users...", toDelete.size());
            userRepository.deleteAll(toDelete);
        }
    }
}