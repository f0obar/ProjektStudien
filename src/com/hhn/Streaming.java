package com.hhn;
import javafx.application.Platform;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Streaming {
    private String oAuthConsumerKey;
    private String  oAuthConsumerSecret;
    private String oAuthAccessToken;
    private String oAuthAccessTokenSecret;
    private final Object lock = new Object();

    private static ArrayList<Status> tweets = new ArrayList<>();

    public Streaming(String oAuthConsumerKey, String oAuthConsumerSecret, String oAuthAccessToken, String oAuthAccessTokenSecret) {
        this.oAuthConsumerKey = oAuthConsumerKey;
        this.oAuthConsumerSecret = oAuthConsumerSecret;
        this.oAuthAccessToken = oAuthAccessToken;
        this.oAuthAccessTokenSecret = oAuthAccessTokenSecret;
    }

    public void streamAndExport(int maxTweets, String[] searchTerms, Controller controller, boolean english, boolean splitTweets) {
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

                controller.updateInformation((double) tweets.size() / maxTweets, status.getText());
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

        FilterQuery tweetFilterQuery = new FilterQuery();
        tweetFilterQuery.track(searchTerms);
        if(english) {
            tweetFilterQuery.language("en");
        }
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
            exportTweets(tweets, splitTweets);
        } catch (IOException exception){};
    }

    private void exportTweets(ArrayList<Status> tweets, Boolean extractHashtags) throws IOException {
        System.out.println("Exporting "+ tweets.size() + " tweets.");
        File file = new File("tweets.csv");
        file.createNewFile();
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8),true);

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
        sb.append(",");
        sb.append("GeoLocation");

        for(Status status : tweets) {
            if(extractHashtags) {
                for(String hashtag: extractHashtags(sanitizeTweetText(status.getText()))) {
                    sb.append(buildRow(status, hashtag));
                }
            } else {
                sb.append(buildRow(status, status.getText()));
            }
        }
        pw.write(sb.toString());
        pw.close();
        System.out.println("tweets exported.");
    }

    private String buildRow(Status status, String text) {
        StringBuilder sb = new StringBuilder();
        sb.append('\n');
        sb.append(status.getId());
        sb.append(',');
        sb.append(sanitizeTweetText(text));
        sb.append(',');
        sb.append(status.getCreatedAt());
        sb.append(',');
        sb.append(status.getLang());
        sb.append(',');
        sb.append(status.getUser().getName());
        sb.append(',');
        sb.append(status.getUser().getFollowersCount());
        sb.append(",");
        if(status.getGeoLocation() != null) {
            sb.append(status.getGeoLocation().getLatitude()+"-"+status.getGeoLocation().getLongitude());
        } else {
            sb.append("-");
        }
        return sb.toString();
    }

    private ArrayList<String> extractHashtags(String tweetText) {
        ArrayList<String> hashtags = new ArrayList<>();
        String[] words = tweetText.split(" ");
        for(String word: words) {
            if(word.startsWith("#")) {
                hashtags.add(word);
            }
        }
        return hashtags;
    }

    private String sanitizeTweetText(String text) {
        return text.replace(',',' ').replace('\n',' ');
    }
}
