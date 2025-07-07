package com.yohan.event_planner.domain;

import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.repository.LabelRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserInitializer {

    private final UserBO userBO;
    private final LabelRepository labelRepository;

    public UserInitializer(UserBO userBO, LabelRepository labelRepository) {
        this.userBO = userBO;
        this.labelRepository = labelRepository;
    }

    @Transactional
    public User initializeUser(User user) {
        User savedUser = userBO.createUser(user);

        Label unlabeled = new Label("Unlabeled", savedUser);

        labelRepository.save(unlabeled);

        savedUser.assignUnlabeled(unlabeled);

        return userBO.updateUser(savedUser);
    }
}
