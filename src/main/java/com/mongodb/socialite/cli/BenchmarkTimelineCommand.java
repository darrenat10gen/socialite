package com.mongodb.socialite.cli;

import com.codahale.metrics.*;
import com.mongodb.*;
import com.mongodb.socialite.ServiceManager;
import com.mongodb.socialite.SocialiteConfiguration;
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
import java.util.concurrent.TimeUnit;


public class BenchmarkTimelineCommand extends ConfiguredCommand<SocialiteConfiguration> {

    public BenchmarkTimelineCommand() {
        super("bench-timeline", "Measure the latency of timeline reads");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);    //To change body of overridden methods use File | Settings | File Templates.
        subparser.addArgument("--iterations").required(true).type(Integer.class);
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
        UserGraphService userService = services.getUserGraphService();
        FeedService feedService = services.getFeedService();

        // Assuming default user service, get collection name
        DefaultUserServiceConfiguration userConfig = 
                (DefaultUserServiceConfiguration) userService.getConfiguration();
        DBCollection user_collection = mongoClient.getDB(default_uri.getDatabase()).
                getCollection(userConfig.user_collection_name);

        DBObject obj = user_collection.findOne(new BasicDBObject(), new BasicDBObject("_id",1),
                new BasicDBObject("_id", -1));
        assert(obj != null);
        int max_user_id = Integer.parseInt((String)obj.get("_id"));
        assert( max_user_id > 0 );

        int iterations = namespace.getInt("iterations");
        assert(iterations>0);

        final MetricRegistry metrics = new MetricRegistry();
        final Timer readLatencies = metrics.timer("timeline-latencies");

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

        while(iterations-- > 0) {
            int user_id = (int)(Math.random()*max_user_id);
            final Timer.Context ctx = readLatencies.time();
            feedService.getFeedFor( new User(String.valueOf(user_id)), 50);
            ctx.stop();
        }

        reporter.stop();
        services.stop();

    }
}