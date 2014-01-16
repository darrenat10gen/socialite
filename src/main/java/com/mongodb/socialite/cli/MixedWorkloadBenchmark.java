package com.mongodb.socialite.cli;

import com.codahale.metrics.*;
import com.mongodb.*;
import com.mongodb.socialite.ServiceManager;
import com.mongodb.socialite.SocialiteConfiguration;
import com.mongodb.socialite.api.Content;
import com.mongodb.socialite.api.User;
import com.mongodb.socialite.configuration.DefaultUserServiceConfiguration;
import com.mongodb.socialite.services.FeedService;
import com.mongodb.socialite.services.UserGraphService;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;

import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import java.io.File;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class MixedWorkloadBenchmark extends ConfiguredCommand<SocialiteConfiguration> {

    public MixedWorkloadBenchmark() {
        super("mixed-workload", "Simulates mixed read/write inbox workload and reports request latency");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);    //To change body of overridden methods use File | Settings | File Templates.
        subparser.addArgument("--proportion").required(true).type(Float.class);
        subparser.addArgument("--threads").required(true).type(Integer.class);
        subparser.addArgument("--seconds").required(true).type(Integer.class);
        subparser.addArgument("--csv").required(false).type(String.class);
    }

    @Override
    protected void run(Bootstrap<SocialiteConfiguration> configBootstrap, Namespace namespace, SocialiteConfiguration config) throws Exception {

        // Create a MongoDB client from the configured URI
        // Get the configured default MongoDB URI
        MongoClientURI default_uri = config.mongodb.default_database_uri;
        MongoClient mongoClient = new MongoClient(default_uri);
        
        // Initialize the services as per configuration
        ServiceManager services = new ServiceManager(config.services, default_uri);
        final UserGraphService userService = services.getUserGraphService();
        final FeedService feedService = services.getFeedService();

        // Assuming default user service, get collection name
        DefaultUserServiceConfiguration userConfig = 
                (DefaultUserServiceConfiguration) userService.getConfiguration();
        DBCollection user_collection = mongoClient.getDB(default_uri.getDatabase()).
                getCollection(userConfig.user_collection_name);

        DBObject obj = user_collection.findOne(new BasicDBObject(), new BasicDBObject("_id",1),
                new BasicDBObject("_id", -1));

        final int max_user_id = Integer.parseInt((String)obj.get("_id"));
        final float rwRatio = namespace.getFloat("proportion");
        final int threads = namespace.getInt("threads");
        final int seconds = namespace.getInt("seconds");


        final MetricRegistry metrics = new MetricRegistry();
        final Timer readLatencies = metrics.timer("read-latencies");
        final Timer writeLatencies = metrics.timer("write-latencies");

        ScheduledReporter reporter = null;

        String dirname = namespace.getString("csv");
        if(dirname != null ) {
            reporter = CsvReporter.forRegistry(metrics)
                    .formatFor(Locale.US)
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .build(new File(dirname));
        } else {
            reporter = ConsoleReporter.forRegistry(metrics)
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .build();
        }

        reporter.start(1, TimeUnit.SECONDS);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        final CountDownLatch latch = new CountDownLatch( threads );

        for(int thread = 0; thread < threads; thread++ ) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    long start = System.currentTimeMillis();

                    while(true) {
                        long now = System.currentTimeMillis();
                        if(now-start >= seconds * 1000) {
                            latch.countDown();
                            return;
                        }
                        int user_id = (int)(Math.random()*max_user_id);
                        final User user = new User(String.valueOf(user_id));
                        if( Math.random() < rwRatio ) {
                            // do a read
                            final Timer.Context ctx = readLatencies.time();
                            feedService.getFeedFor( user, 50);
                            ctx.stop();
                        } else {
                            // do a write
                            final Content content = new Content( user, randomString(), null );
                            final Timer.Context ctx = writeLatencies.time();
                            feedService.post(user, content);
                            ctx.stop();
                        }
                    }
                }
            });
        };

        latch.await();
        executor.shutdown();
        executor.awaitTermination(10,TimeUnit.SECONDS);
        reporter.stop();
        services.stop();
    }

    private static final char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    private Random random = new Random();

    protected String randomString() {
        StringBuilder sb = new StringBuilder();
        for( int i = 0; i < 140; i++ ) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        return sb.toString();
    }
}