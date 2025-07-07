package com.yohan.event_planner.repository;

import com.yohan.event_planner.domain.LabelTimeBucket;
import com.yohan.event_planner.domain.enums.TimeBucketType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LabelTimeBucketRepository extends JpaRepository<LabelTimeBucket, Long> {
    Optional<LabelTimeBucket> findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
            Long userId, Long labelId, TimeBucketType bucketType, int bucketYear, int bucketValue
    );

    List<LabelTimeBucket> findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
            Long userId,
            Collection<Long> labelIds,
            TimeBucketType bucketType,
            int bucketYear,
            List<Integer> bucketValues
    );

    List<LabelTimeBucket> findByUserIdAndLabelIdIn(
            Long userId,
            Collection<Long> labelIds
    );
}
