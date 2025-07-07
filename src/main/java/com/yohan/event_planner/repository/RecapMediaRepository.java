package com.yohan.event_planner.repository;

import com.yohan.event_planner.domain.RecapMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecapMediaRepository extends JpaRepository<RecapMedia, Long> {

    /**
     * Finds all media items for a given recap, ordered by their mediaOrder field.
     *
     * @param recapId the ID of the recap
     * @return list of media items ordered by mediaOrder
     */
    List<RecapMedia> findByRecapIdOrderByMediaOrder(Long recapId);

    /**
     * Finds all media items for a given recap without enforcing order.
     *
     * @param recapId the ID of the recap
     * @return list of media items
     */
    List<RecapMedia> findByRecapId(Long recapId);

    /**
     * Counts the number of media items attached to a recap.
     *
     * @param recapId the ID of the recap
     * @return the count of media items
     */
    int countByRecapId(Long recapId);

    /**
     * Deletes all media items associated with the given recap ID.
     *
     * @param recapId the ID of the recap
     */
    void deleteByRecapId(Long recapId);
}
