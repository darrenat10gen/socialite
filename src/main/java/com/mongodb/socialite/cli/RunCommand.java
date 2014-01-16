package com.mongodb.socialite.cli;

import com.mongodb.MongoClientURI;
import com.mongodb.socialite.ServiceManager;
import com.mongodb.socialite.SocialiteConfiguration;
import com.mongodb.socialite.api.Content;
import com.mongodb.socialite.benchmark.traffic.TrafficGenerator;
import com.mongodb.socialite.benchmark.traffic.UserCommand;
import com.mongodb.socialite.benchmark.traffic.WorkingSetTrafficGenerator;
import com.mongodb.socialite.services.FeedService;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;

import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class RunCommand extends ConfiguredCommand<SocialiteConfiguration> {

    public RunCommand() {
        super("run", "Runs a synthetic workload benchmark");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);    //To change body of overridden methods use File | Settings | File Templates.
        subparser.addArgument("--totalusers").required(true).type(Integer.class);
        subparser.addArgument("--concurrentusers").required(true).type(Integer.class);
        subparser.addArgument("--threads").required(true).type(Integer.class);
        subparser.addArgument("--seconds").required(true).type(Integer.class);
    }

    @Override
    protected void run(Bootstrap<SocialiteConfiguration> configBootstrap, 
            Namespace namespace, SocialiteConfiguration config) throws Exception {

        // Get the configured default MongoDB URI
        MongoClientURI default_uri = config.mongodb.default_database_uri;
        
        // Initialize the services as per configuration
        ServiceManager services = new ServiceManager(config.services, default_uri);
        final FeedService feedService = services.getFeedService();

        final int threads = namespace.getInt("threads");
        final int totalUsers = namespace.getInt("totalusers");
        final int concurrentUsers = namespace.getInt("concurrentusers");
        final int seconds = namespace.getInt("seconds");

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(threads);
        final TrafficGenerator generator = new WorkingSetTrafficGenerator(concurrentUsers,totalUsers);

        int targetRate = generator.getTargetRate();
        final double perThreadRate = targetRate / threads;
        List<ScheduledFuture<?>> futures = new ArrayList<ScheduledFuture<?>>();

        int sleepMs = (int)((1/perThreadRate) * 1000);

        System.out.println("Target rate: " + targetRate);
        System.out.println("Running " + threads + " threads with period of " + sleepMs + " each");

        for( int i = 0; i < threads; i++ ) {
            final Runnable r = new Runnable() {
                public void run() {
                    UserCommand cmd = generator.next();
                    switch(cmd.type) {
                        case POST: {
                            feedService.post( cmd.user, new Content(cmd.user, randomString(), null));
                            break;
                        }
                        case READ_TIMELINE: {
                            feedService.getFeedFor(cmd.user, 50);
                            break;
                        }
                    }
                }
            };
            futures.add(executor.scheduleAtFixedRate(r,0l,sleepMs,TimeUnit.MILLISECONDS));
        }

        Thread.sleep( seconds * 1000 );

        for( ScheduledFuture<?> f : futures ) {
            f.cancel(true);
        }

        executor.shutdown();
        executor.awaitTermination(10,TimeUnit.SECONDS);
        services.stop();
    }

    private static final char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    private Random random = new Random();
    protected String randomString() {

        int length = Math.abs(10 + random.nextInt(130));

        StringBuilder sb = new StringBuilder();
        for( int i = 0; i < length; i++ ) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        return sb.toString();
    }

}