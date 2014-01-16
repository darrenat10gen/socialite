package com.mongodb.socialite.benchmark.traffic;

import com.mongodb.socialite.api.User;
import com.mongodb.socialite.services.FeedService;
import com.mongodb.socialite.services.UserGraphService;

import java.util.Random;

public class UserCommand {

    public User user;
    public Type type;

    public enum Type {
        POST,
        READ_TIMELINE
    };

    public UserCommand(User u, Type t) {
        this.user = u;
        this.type = t;
    }

}
