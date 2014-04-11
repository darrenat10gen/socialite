A Scalable Social Status Feed
=============================

The system has a number of users and each can "follow" zero or more other users. Users can send messages to their followers.
 Users have a timeline that contains the messages from people they follow. Each user also has a "posts" resource that
 contains the messages they have sent.

The service is implemented using [dropwizard](http://www.dropwizard.io).

REST API
--------

    GET     /users/{user_id}                     Get a User by their ID
    DELETE  /users/{user_id}                     Remove a user by their ID
    POST    /users/{user_id}/posts               Send a message from this user
    GET     /users/{user_id}/followers           Get a list of followers of a user
    GET     /users/{user_id}/followers_count     Get the number of followers of a user
    GET     /users/{user_id}/following           Get the list of users this user is following
    GET     /users/{user_id}/following_count     Get the number of users this user follows
    GET     /users/{user_id}/posts               Get the messages sent by a user
    GET     /users/{user_id}/timeline            Get the timeline for this user
    PUT     /users/{user_id}                     Create a new user
    PUT     /users/{user_id}/following/{target}  Follow a user
    DELETE  /users/{user_id}/following/{target}  Unfollow a user

Using the API
==================

Create some users
-------------
    $ curl -X PUT localhost:8080/users/jsr
    {"_id":"jsr"}

    $ curl -X PUT localhost:8080/users/darren
    {"_id":"darren"}

    $ curl -X PUT localhost:8080/users/ian
    {"_id":"ian"}

Add some following relationships
--------------------------------

    Ian and Darren follow jared
    $ curl -X PUT localhost:8080/users/ian/following/jsr
    $ curl -X PUT localhost:8080/users/darren/following/jsr

    Get jsr's followers
    $ curl localhost:8080/users/jsr/followers
    [{"_id":"darren"},{"_id":"ian"}]

    Find who darren is following
    $ curl localhost:8080/users/darren/following
    [{"_id":"jsr"}]

Send a message
------------------

    Jared sends a message
    $ curl -X POST localhost:8080/users/jsr/posts?message=hello
    {"author":"jsr","_id":"525e2b01a0eeecc56300019d"}

Get a user's timeline
---------------------

    Get darren's timeline
    $ curl localhost:8080/users/darren/timeline
    [{"message":"still here","date":1381903352000,"author":"jsr","_id":"525e2bf8a0eeecc56300019f"},
     {"message":"hello again","date":1381903320000,"author":"jsr","_id":"525e2bd8a0eeecc56300019e"},
     {"message":"hello","date":1381903105000,"author":"jsr","_id":"525e2b01a0eeecc56300019d"}]

    Get jared's posts
    $ curl localhost:8080/users/jsr/posts
    [{"message":"still here","date":1381903352000,"author":"jsr","_id":"525e2bf8a0eeecc56300019f"},
     {"message":"hello again","date":1381903320000,"author":"jsr","_id":"525e2bd8a0eeecc56300019e"},
     {"message":"hello","date":1381903105000,"author":"jsr","_id":"525e2b01a0eeecc56300019d"}]

Running the service
===================

- Duplicate sample.yml and complete the configuration file
- Run "java -jar ./target/socialite-0.0.1-SNAPSHOT.jar server sample-config.yml"
- Service is now running on port 8080

Benchmarking Tools
==================
This project includes utilities to help benchmark and test. It's packaged in the form
of commands that are built into the Socialite service. The following commands are supported

Bulk Loading Data
------------------
the "load" command can be used to bulk load users, followers, and messages into the system.

    $ java -jar ./target/socialite-0.0.1-SNAPSHOT.jar load
    usage: java -jar socialite-0.0.1-SNAPSHOT.jar
           load [-h] --users USERS --maxfollows MAXFOLLOWERS --messages MESSAGES --threads THREADS [file]

The options are:

    --users (integer)
specifies the number of users to create. they'll be created with names as stringified integers in the range "[0..USERS]"

    --maxfollows (integer)
specifies the maximum number of users each user can follow. the tool picks a zipfian distributed random number
between [0..maxfollows] of users to follow. it will then choose a zipfian distributed random user to follow in the
range [0..current graph size].

    --messages (integer)
specifies how many messages should be sent from each user. after the users have been created,
and the followers added, the command will send this many messages from each user in the system with random content.

    --threads (integer) 
how many threads to use. the loader creates a FIFO queue of tasks and a threadpool of this size will be used to actually 
load the data in.

    [file] optional path to configuration file
The load command uses the same configuration file as the socialite service itself. this allows you to configure
which implementation of the graph, user, and content service to use for the test. if you don't specify a file,
the default options will be used.

a sample invocation of the command

    $ java -jar ./target/socialite-0.0.1-SNAPSHOT.jar load --users 1000 --maxfollows 100 --messages 100

This will create 1000 users. Each user will have around 100 followers. Each user will have sent 100 messages.

Benchmark Timeline Read Latency
-------------------------------
The "bench-timeline" command can be used to measure the latency of timeline read operations. The tool assumes that
you used the "Bulk Load" command to load up the database. When it starts up, it will read the max_user_id from the
user service and use that as the upper bound of the user ID's that it randomly selects. It will select user ID's in
the range 0..MAX;

    $ java -jar ./target/socialite-0.0.1-SNAPSHOT.jar bench-timeline
    usage: java -jar socialite-0.0.1-SNAPSHOT.jar bench-timeline [-h] --iterations ITERATIONS [--csv CSV] [file]

it supports the following parameters

    --iterations (integer)
This is how many timeline reads will be performed over the run of the test. The tool will pick random user_id's and
read their timeline until it's reached this limit.

    --csv (directory-name) (optional)
If you omit this value, the tool will periodically print performance stats to STDOUT while it's running. Each outline
 will look like this:

    10/17/13 1:35:26 PM ============================================================

    -- Timers ----------------------------------------------------------------------
    timeline-latencies
                 count = 8885
             mean rate = 145.65 calls/second
         1-minute rate = 141.94 calls/second
         5-minute rate = 137.08 calls/second
        15-minute rate = 135.86 calls/second
                   min = 1.43 milliseconds
                   max = 36.10 milliseconds
                  mean = 6.62 milliseconds
                stddev = 4.89 milliseconds
                median = 5.12 milliseconds
                  75% <= 8.84 milliseconds
                  95% <= 16.02 milliseconds
                  98% <= 18.98 milliseconds
                  99% <= 22.50 milliseconds
            99.9% <= 36.09 milliseconds

If a value of csv is specified, the tool will append lines to a CSV file in that directory. The directory must exist
before the tool starts.

Mixed Workload Benchmark
-------------------------------
The "mixed-workload" command can be used to measure the latency of timeline read and write operations. The tool
assumes that you used the "Bulk Load" command to load up the database. When it starts up, it will read the max_user_id from the
user service and use that as the upper bound of the user ID's that it randomly selects. It will select user ID's in
the range 0..MAX; It will randomly select a read or write operation to perform based on the proportion attribute.

    $ java -jar ./target/socialite-0.0.1-SNAPSHOT.jar mixed-workload
    usage: java -jar socialite-0.0.1-SNAPSHOT.jar
        mixed-workload [-h] --proportion PROPORTION --threads THREADS --seconds SECONDS [--csv CSV] [file]

it supports the following parameters

    -- proportion (float 0..1)
Specifies the ratio of read to write operations. For example, if the value is set to .8, the test will run 80% reads,
 20% writes.

    --threads (integer)
How many threads to run. Each thread will run the specified proportion of read/write operations. 

    --seconds (integer) 
The number of seconds to run the test for. 

    --csv (directory-name) (optional)
If you omit this value, the tool will periodically print performance stats to STDOUT while it's running. Each outline
 will look like this:

    10/17/13 1:35:26 PM ============================================================

    -- Timers ----------------------------------------------------------------------
    read-latencies
                 count = 8885
             mean rate = 145.65 calls/second
         1-minute rate = 141.94 calls/second
         5-minute rate = 137.08 calls/second
        15-minute rate = 135.86 calls/second
                   min = 1.43 milliseconds
                   max = 36.10 milliseconds
                  mean = 6.62 milliseconds
                stddev = 4.89 milliseconds
                median = 5.12 milliseconds
                  75% <= 8.84 milliseconds
                  95% <= 16.02 milliseconds
                  98% <= 18.98 milliseconds
                  99% <= 22.50 milliseconds
            99.9% <= 36.09 milliseconds
    write-latencies
                 count = 8885
             mean rate = 145.65 calls/second
         1-minute rate = 141.94 calls/second
         5-minute rate = 137.08 calls/second
        15-minute rate = 135.86 calls/second
                   min = 1.43 milliseconds
                   max = 36.10 milliseconds
                  mean = 6.62 milliseconds
                stddev = 4.89 milliseconds
                median = 5.12 milliseconds
                  75% <= 8.84 milliseconds
                  95% <= 16.02 milliseconds
                  98% <= 18.98 milliseconds
                  99% <= 22.50 milliseconds
            99.9% <= 36.09 milliseconds

If a value of csv is specified, the tool will append lines to a CSV file in that directory. The directory must exist
before the tool starts.

Configuration Options
=====================

    mongodb:
        default_database_uri : Each service that requires a database may be configured individually with a
                               URI which represents its specific connection. If a service is not configured
                               with a specific URI, this default is used.

    services:

        <common to all services>
            database_uri                  : override the default uri for this specific service
            database_name                 : used only if there is no database name specified by configured uri

        async_service:
            model: DefaultAsyncService    : implementation to use for Async Service (default: null (none))
            service_signature             : unique identifier for this service instance (generated by default)
            recovery_collection_name      : name of collection for storing asyncronous work (default: "async_recovery") 
            persist_rejected_tasks        : when tasks cannot be processed or queued, persist for later processing (default: true)
            processing_thread_pool_size   : size of thread pool for processing async tasks (default: 4)
            async_tasks_max_queue_size    : size of in memory task queue (default: 1000)
            recovery_poll_time            : poll time (ms) for finding persisted async tasks to process (default: 3000)
                                            use -1 to never process persistently queued tasks or recover failed ones
            failure_recovery_timeout      : time after which the async processor will declared a processing task to
                                            be hung/failed and attempt to reprocess it. Use -1 (default) to disable.
            max_task_failures             : maximum times a task can fail before recovery attempts stop (default: 3)

        content_service:
            model: DefaultContentService  : implementation to use for Content Service (default: "DefaultContentService")
            content_collection_name       : name of collection where messages are stored
            content_validation_class      : java class name of validation implementation to use

        user_graph_service: 
            model: DefaultUserService     : implementation to use for User Service (default: "DefaultUserService")
            maintain_follower_collection  : whether or not to store forward follower links (default: true)
            follower_collection_name      : name of collection where follower information is stored
            maintain_following_collection : whether or not to store the reverse follower links (default: true)
            following_collection_name     : name of the collection where reverse links are stored
            maintain_reverse_index        : use a reverse index on follower collection. useful for un-sharded  deployments
            store_follow_counts_with_user : maintain a count of followers in the user object (default: false)
            user_collection_name          : name of collection where user objects are stored
            user_validation_class         : java class to use to validate user records

        feed_processing:
            model: AsyncPostDelivery      : the feed processor to use, default is no processor which means 
                                            user service requests go directly to the feed service. Using
                                            AsyncPostDelivery intercepts high fanout posts and processes them
                                            via the Async service
            async_fanout_threshold        : fanout threshold, above which posts become asynchronous (default: 200)

        feed_service:

            model: FanoutOnRead           : implementation to use for Feed Service (default: "FanoutOnRead")
            fanout_limit                  : limit users visible in feed, regardless of follower count
            
            model: FanoutOnWriteSizedBuckets  
            fanout_limit                  : limit users visible in feed, regardless of follower count
            bucket_size                   : maximum bucket size (specified in messages) in cache (default: 50)
            bucket_read_batch_size        : number of buckets pulled from database at a time on read (default: 2)
            bucket_collection_name        : MongoDB collection name for the buckets store (default: "sized_buckets")
            cache_author                  : cache the content author in the bucket cache (default: true)
            cache_message                 : cache the content message in the bucket cache (default: true)
            cache_data                    : cache the content data the bucket cache (default: true)

            model: FanoutOnWriteTimeBuckets  
            fanout_limit                  : limit users visible in feed, regardless of follower count
            bucket_timespan_days          : maximum time range of messages in single bucket (default: 7)
            bucket_read_batch_size        : number of buckets pulled from database at a time on read (default: 2)
            bucket_collection_name        : MongoDB collection name for the buckets store (default: "timed_buckets")
            bucket_user_posts             : also store a users posts in their timeline bucket cache (default: true) 
            cache_author                  : cache the content author in the bucket cache (default: true)
            cache_message                 : cache the content message in the bucket cache (default: true)
            cache_data                    : cache the content data the bucket cache (default: true)

            model: FanoutOnWriteToCache  
            fanout_limit                  : limit users visible in feed, regardless of follower count
            cache_size_limit              : maximum number of posts in the timeline cache per user (default: 50)
            bucket_collection_name        : MongoDB collection name for the buckets store (default: "timeline_cache")
            bucket_user_posts             : also store a users posts in their timeline bucket cache (default: true) 
            cache_author                  : cache the content author in the timeline cache (default: true)
            cache_message                 : cache the content message in the timeline cache (default: true)
            cache_data                    : cache the content data the timeline cache (default: true)

