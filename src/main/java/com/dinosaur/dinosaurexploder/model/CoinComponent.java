package com.dinosaur.dinosaurexploder.model;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.almasb.fxgl.entity.component.Component;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class CoinComponent extends Component {
    Integer coin = 0;
    private final Integer COIN_VALUE = 1;

    public static int totalCoins;

    private final LanguageManager languageManager = LanguageManager.getInstance();

    private Text coinText;
    private Node coinUI;

    @Override
    public void onAdded() {
        loadCoins(); // Deserialize once when the component is added

        // Create UI elements
        coinText = new Text();


        coinText.setFill(Color.GOLDENROD);
        coinText.setFont(Font.font(GameConstants.ARCADECLASSIC_FONTNAME, 20));

        coinText.setLayoutX(0);
        coinText.setLayoutY(0);




        coinUI = createCoinUI();
        entity.getViewComponent().addChild(coinUI);
    }


    private void updateText(){
        coinText.setText(languageManager.getTranslation("coin")+"\t"+coin);
    }

    private void loadCoins() {
        try (FileInputStream file = new FileInputStream("totalCoins.ser");
             ObjectInputStream in = new ObjectInputStream(file)) {
            totalCoins = (Integer) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            totalCoins = 0;
        }
    }


    private Node createCoinUI(){
        var container = new HBox(5);
        Image image = new Image(GameConstants.COIN_IMAGEPATH, 25, 20, false, false);
        ImageView imageView = new ImageView(image);
        container.getChildren().addAll(coinText, imageView);
        return container;

    }

    private void saveCoinData() {
        try (FileOutputStream file = new FileOutputStream("totalCoins.ser");
             ObjectOutputStream out = new ObjectOutputStream(file)) {
            out.writeObject(totalCoins);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdate(double tpf) {
//        update coin UI text
        updateText();
    }


    public Integer getCoin() {
        return coin;
    }

    public void setCoin(Integer coin) {
        this.coin = coin;
        updateText();
    }

    // public void incrementCoin(){
    //     coin += COIN_VALUE;
    //     totalCoins += COIN_VALUE;  // Update total coins
    //     updateText();

    //     saveCoinData();
    // }
    public void incrementCoin() {
        coin += COIN_VALUE;     // Increment the coin count for the player
        totalCoins += COIN_VALUE;  // Increment the total coins
        updateText();           // Update the coin display on the UI

        // Save total coins to file after every increment
        try (FileOutputStream file = new FileOutputStream("totalCoins.ser");
            ObjectOutputStream out = new ObjectOutputStream(file)) {
            out.writeObject(totalCoins); // Save the total coins to file
        } catch (IOException e) {
            e.printStackTrace();  // Handle errors during saving
        }
}
}