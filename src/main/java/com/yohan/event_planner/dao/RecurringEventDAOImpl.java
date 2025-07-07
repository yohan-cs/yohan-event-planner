package com.yohan.event_planner.dao;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.PagedList;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.enums.TimeFilter;
import com.yohan.event_planner.dto.RecurringEventFilterDTO;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

import static com.yohan.event_planner.time.TimeUtils.FAR_PAST_DATE;
import static com.yohan.event_planner.time.TimeUtils.FAR_FUTURE_DATE;

@Repository
public class RecurringEventDAOImpl implements RecurringEventDAO {

    private final CriteriaBuilderFactory cbf;
    private final EntityManager em;

    public RecurringEventDAOImpl(
            CriteriaBuilderFactory cbf,
            EntityManager em
    ) {
        this.cbf = cbf;
        this.em = em;
    }

    @Override
    public PagedList<RecurringEvent> findConfirmedRecurringEvents(Long userId, RecurringEventFilterDTO filter, int pageNumber, int pageSize) {
        CriteriaBuilder<RecurringEvent> cb = cbf.create(em, RecurringEvent.class)
                .fetch("creator")
                .fetch("label");

        applyUserFilter(userId, cb);
        applyOnlyConfirmedFilter(cb);
        applyLabelFilter(filter, cb);
        applyTimeWindowFilter(filter, cb);
        applySortOrder(filter, cb);

        return cb.page(pageNumber, pageSize).getResultList();
    }

    private void applyUserFilter(Long userId, CriteriaBuilder<RecurringEvent> cb) {
        cb.where("creator.id").eq(userId);
    }

    private void applyOnlyConfirmedFilter(CriteriaBuilder<RecurringEvent> cb) {
        cb.where("unconfirmed").eq(false);
    }

    private void applyLabelFilter(RecurringEventFilterDTO filter, CriteriaBuilder<RecurringEvent> cb) {
        if (filter.labelId() != null) {
            cb.where("label.id").eq(filter.labelId());
        }
    }

    private void applyTimeWindowFilter(RecurringEventFilterDTO filter, CriteriaBuilder<RecurringEvent> cb) {
        // Service layer has already resolved TimeFilter to actual dates
        LocalDate startDate = filter.startDate();
        LocalDate endDate = filter.endDate();
        
        // Filter for recurring events that overlap with the time window
        cb.where("startDate").le(endDate);
        cb.where("endDate").ge(startDate);
    }

    private void applySortOrder(RecurringEventFilterDTO filter, CriteriaBuilder<RecurringEvent> cb) {
        if (Boolean.TRUE.equals(filter.sortDescending())) {
            cb.orderByDesc("startDate")
                    .orderByDesc("endDate")
                    .orderByDesc("id");
        } else {
            cb.orderByAsc("startDate")
                    .orderByAsc("endDate")
                    .orderByAsc("id");
        }
    }
}
