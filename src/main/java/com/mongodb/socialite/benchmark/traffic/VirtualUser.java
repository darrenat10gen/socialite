package com.mongodb.socialite.benchmark.traffic;

import com.mongodb.socialite.api.User;

import java.util.Random;

public class VirtualUser {

    public static final int AVG_OPS=25;
    public static final int PERCENT_WRITES=25;

    private User user;
    private int opsRemaining;
    private Random rnd = new Random();

    public VirtualUser(int id) {
        this.user = new User(String.valueOf(id));
        this.opsRemaining = (int)(Math.random() * 2 * AVG_OPS);
    }

    public boolean hasNext() {
        return opsRemaining > 0;
    }

    public UserCommand next() {
        opsRemaining--;
        int r = rnd.nextInt(100);
        if( r < PERCENT_WRITES ) {
            return new UserCommand(user,UserCommand.Type.POST);
        } else {
            return new UserCommand(user,UserCommand.Type.READ_TIMELINE);
        }
    }

}
