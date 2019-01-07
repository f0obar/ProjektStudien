package com.hhn;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Controller {

    @FXML
    private Slider TweetCountSlider;

    @FXML
    private TextField SearchTermField;

    @FXML
    private ProgressBar Progress;

    @FXML
    private TextArea Preview;

    @FXML
    private CheckBox englishBtn;

    @FXML
    void start(ActionEvent event) {
        String oAuthConsumerKey = "";
        String oAuthConsumerSecret = "";
        String oAuthAccessToken = "";
        String oAuthAccessTokenSecret = "";

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
        Streaming streaming = new Streaming(oAuthConsumerKey,oAuthConsumerSecret,oAuthAccessToken,oAuthAccessTokenSecret);
        String[] searchTerms = SearchTermField.getText().split(",");
        Boolean english = englishBtn.isSelected();

        new Thread(() -> streaming.streamAndExport((int)TweetCountSlider.getValue(),searchTerms,this, english)).start();
    }

    public void updateInformation(double progressBar, String currentTweet) {
        Platform.runLater(()-> {
            Preview.setText(currentTweet);
            Progress.setProgress(progressBar);
        });
    }
}
