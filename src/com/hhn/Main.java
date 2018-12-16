package com.hhn;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.io.*;
import java.util.ArrayList;

public class Main {
    private static String oAuthConsumerKey;
    private static String  oAuthConsumerSecret;
    private static String oAuthAccessToken;
    private static String oAuthAccessTokenSecret;
    private static int maxTweets = 10;
    private static final Object lock = new Object();

    private static ArrayList<Status> tweets = new ArrayList<>();


    public static void main(String[] args) {
        if(args.length > 0) {
            maxTweets = Integer.valueOf(args[0]);
        }
        System.out.println("Collecting " + maxTweets + " tweets");
        try (BufferedReader br = new BufferedReader(new FileReader("credentials.txt"))) {
            String line;
            oAuthConsumerKey = br.readLine();
            oAuthConsumerSecret = br.readLine();
            oAuthAccessToken = br.readLine();
            oAuthAccessTokenSecret = br.readLine();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.setOAuthConsumerKey(oAuthConsumerKey)
                .setOAuthConsumerSecret(oAuthConsumerSecret)
                .setOAuthAccessToken(oAuthAccessToken)
                .setOAuthAccessTokenSecret(oAuthAccessTokenSecret);

        TwitterStream twitterStream = new TwitterStreamFactory(configurationBuilder.build()).getInstance();

        twitterStream.addListener(new StatusListener() {
            @Override
            public void onStatus(Status status) {
                tweets.add(status);
                System.out.println("found new status, collected " + tweets.size() + "/" + maxTweets + " tweets");
                if(tweets.size() >= maxTweets) {
                    synchronized (lock) {
                        lock.notify();
                    }
                }
            }

            @Override
            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {

            }

            @Override
            public void onTrackLimitationNotice(int i) {

            }

            @Override
            public void onScrubGeo(long l, long l1) {

            }

            @Override
            public void onStallWarning(StallWarning stallWarning) {

            }

            @Override
            public void onException(Exception e) {

            }
        });

        FilterQuery tweetFilterQuery = new FilterQuery(); // See
        tweetFilterQuery.track(new String[]{"#trump","#donaldtrump","#realdonaldtrump","@realdonaldtrumo"}); // OR on keywords
        tweetFilterQuery.language(new String[]{"en"}); // Note that language does not work properly on Norwegian tweets

        twitterStream.filter(tweetFilterQuery);

        try {
            synchronized (lock) {
                lock.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("returning statuses");
        twitterStream.shutdown();
        try {
            exportTweets(tweets);
        } catch (IOException exception){};
    }

    private static void exportTweets(ArrayList<Status> tweets) throws IOException {
        System.out.println("Exporting "+ tweets.size() + " tweets.");
        File file = new File("tweets.csv");
        //file.getParentFile().mkdirs();
        file.createNewFile();

        PrintWriter pw = new PrintWriter(file);

        StringBuilder sb = new StringBuilder();
        sb.append("TweetId");
        sb.append(',');
        sb.append("Text");
        sb.append(',');
        sb.append("CreatedAt");
        sb.append(',');
        sb.append("Lang");
        sb.append(',');
        sb.append("UserName");
        sb.append(',');
        sb.append("UserFollowers");
        for(Status status : tweets) {
            sb.append('\n');
            sb.append(status.getId());
            sb.append(',');
            sb.append(status.getText().replace(',',' ').replace('\n',' '));
            sb.append(',');
            sb.append(status.getCreatedAt());
            sb.append(',');
            sb.append(status.getLang());
            sb.append(',');
            sb.append(status.getUser().getName());
            sb.append(',');
            sb.append(status.getUser().getFollowersCount());
        }
        pw.write(sb.toString());
        pw.close();
        System.out.println("tweets exported.");
    }
}
