package com.mongodb.socialite.benchmark.traffic;

import com.mongodb.socialite.api.User;
import com.mongodb.socialite.benchmark.traffic.TrafficGenerator;
import com.mongodb.socialite.benchmark.traffic.UserCommand;
import com.mongodb.socialite.benchmark.traffic.WorkingSetTrafficGenerator;

import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class WorkingSetTrafficGeneratorTest {

    int GRAPH_SIZE = 10;
    int WS_SIZE = 5;

    @Test
    public void shouldReturnAStreamOfEvents() {
        TrafficGenerator generator = new WorkingSetTrafficGenerator(WS_SIZE,GRAPH_SIZE);
        for( int i = 0; i < 100; i++ ) {
            UserCommand cmd = generator.next();
            assertTrue( Integer.decode( cmd.user.getUserId() ) < GRAPH_SIZE );
        }
    }

}
