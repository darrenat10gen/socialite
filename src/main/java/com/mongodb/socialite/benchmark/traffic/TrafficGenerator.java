package com.mongodb.socialite.benchmark.traffic;

import java.util.Iterator;

public interface TrafficGenerator extends Iterator<UserCommand> {

    public int getTargetRate();
}
