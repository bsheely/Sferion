package com.sferion.whitewater.backend.admin;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sferion.whitewater.backend.domain.User;
import com.sferion.whitewater.backend.domain.enums.UserRole;
import com.sferion.whitewater.ui.SessionData;
import org.apache.onami.persist.EntityManagerProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PiggingUserAdmin {
    private final EntityManagerProvider entityManagerProvider;
    private final Provider<SessionData> sessionDataProvider;

    @Inject
    public PiggingUserAdmin(EntityManagerProvider entityManagerProvider, Provider<SessionData> sessionDataProvider) {
        this.entityManagerProvider = entityManagerProvider;
        this.sessionDataProvider = sessionDataProvider;
    }

    public List<String> getUserNames() {
        List<User> users = getUsers();
        List<String> userNames = new ArrayList<>();
        for (User user : users)
            userNames.add(user.getName() + " " + user.getLastName());
        return userNames;
    }

    private List<User> getUsers() {
        User currentUser = sessionDataProvider.get().getCurrentUser();
        if (currentUser != null && currentUser.getUserRole().equals(UserRole.SystemAdmin)) {
            return entityManagerProvider.get().createQuery("from User where enabled=true", User.class)
                    .getResultList();
        } else if (currentUser != null && currentUser.getCompany() != null)
            return entityManagerProvider.get().createQuery("from User where enabled=true and company = :c", User.class)
                    .setParameter("c", currentUser.getCompany())
                    .getResultList();
        else
            return Collections.emptyList();
    }
}
