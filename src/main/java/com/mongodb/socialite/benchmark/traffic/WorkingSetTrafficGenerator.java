package com.mongodb.socialite.benchmark.traffic;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WorkingSetTrafficGenerator implements TrafficGenerator {

    private List<VirtualUser> activeUsers = new ArrayList<VirtualUser>();
    private Random rand = new Random();
    private int graphSize;
    private int workingSetSize;

    public WorkingSetTrafficGenerator(int workingSetSize, int graphSize) {
        this.graphSize = graphSize;
        this.workingSetSize = workingSetSize;
        for( int i = 0; i < workingSetSize; i++ ) {
            int userId = rand.nextInt(graphSize);
            VirtualUser vuser = new VirtualUser(userId);
            activeUsers.add(vuser);
        }
    }

    @Override
    public int getTargetRate() {
        return Math.max((int)(workingSetSize *  0.0005),1); // this is ratio of rate / user size from twitter
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public synchronized UserCommand next() {
        int thisUser = rand.nextInt(workingSetSize);
        VirtualUser user = activeUsers.get(thisUser);

        // If this user has run out of actions, pick a new random user to replace them
        if(user.hasNext()) {
            return user.next();
        } else {
            int newUserId = rand.nextInt(graphSize);
            VirtualUser newUser = new VirtualUser(newUserId);
            activeUsers.set(thisUser, newUser);
            return newUser.next();
        }
    }

    @Override
    public void remove() {
        throw new NotImplementedException();
    }
}
