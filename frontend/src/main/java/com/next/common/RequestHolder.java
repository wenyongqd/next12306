package com.next.common;

import com.next.model.TrainUser;

public class RequestHolder {

    private static final ThreadLocal<TrainUser> userHolder = new ThreadLocal<>();

    public static void add(TrainUser trainUser) {
        userHolder.set(trainUser);
    }

    public static TrainUser getCurrentUser() {
        return userHolder.get();
    }

    public static void remove() {
        userHolder.remove();
    }
}
