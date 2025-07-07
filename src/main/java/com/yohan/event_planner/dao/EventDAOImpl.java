package com.yohan.event_planner.dao;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.PagedList;
import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.EventFilterDTO;
import com.yohan.event_planner.exception.UserNotFoundException;
import com.yohan.event_planner.time.ClockProvider;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.ZonedDateTime;

import static com.yohan.event_planner.time.TimeUtils.FAR_FUTURE;
import static com.yohan.event_planner.time.TimeUtils.FAR_PAST;

@Repository
public class EventDAOImpl implements EventDAO {

    private final CriteriaBuilderFactory cbf;
    private final EntityManager em;
    private final UserBO userBO;
    private final ClockProvider clockProvider;

    public EventDAOImpl(
            CriteriaBuilderFactory cbf,
            EntityManager em,
            UserBO userBO,
            ClockProvider clockProvider
    ) {
        this.cbf = cbf;
        this.em = em;
        this.userBO = userBO;
        this.clockProvider = clockProvider;
    }

    @Override
    public PagedList<Event> findConfirmedEvents(Long userId, EventFilterDTO filter, int pageNumber, int pageSize) {
        User targetUser = userBO.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Clock userClock = clockProvider.getClockForUser(targetUser);
        ZonedDateTime now = ZonedDateTime.now(userClock);

        CriteriaBuilder<Event> cb = cbf.create(em, Event.class)
                .fetch("creator")
                .fetch("label");

        applyUserFilter(userId, cb);
        applyOnlyConfirmedFilter(cb);
        applyLabelFilter(filter, cb);
        applyTimeWindowFilter(filter, cb, now);
        applyIncompletePastEventFilter(filter, cb, now);
        applySortOrder(filter, cb);

        return cb.page(pageNumber, pageSize).getResultList();
    }

    private void applyUserFilter(Long userId, CriteriaBuilder<Event> cb) {
        cb.where("creator.id").eq(userId);
    }

    private void applyOnlyConfirmedFilter(CriteriaBuilder<Event> cb) {
        cb.where("unconfirmed").eq(false);
    }

    private void applyLabelFilter(EventFilterDTO filter, CriteriaBuilder<Event> cb) {
        if (filter.labelId() != null) {
            cb.where("label.id").eq(filter.labelId());
        }
    }

    private void applyTimeWindowFilter(EventFilterDTO filter, CriteriaBuilder<Event> cb, ZonedDateTime now) {
        cb.where("startTime").ge(filter.start());
        cb.where("endTime").le(filter.end());
    }

    private void applyIncompletePastEventFilter(EventFilterDTO filter, CriteriaBuilder<Event> cb, ZonedDateTime now) {
        if (Boolean.FALSE.equals(filter.includeIncompletePastEvents())) {
            cb.whereOr()
                    .where("endTime").gt(now)
                    .where("isCompleted").eq(true)
                    .endOr();
        }
    }

    private void applySortOrder(EventFilterDTO filter, CriteriaBuilder<Event> cb) {
        if (Boolean.TRUE.equals(filter.sortDescending())) {
            cb.orderByDesc("startTime").orderByDesc("id");
        } else {
            cb.orderByAsc("startTime").orderByAsc("id");
        }
    }
}
