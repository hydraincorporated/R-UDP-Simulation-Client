package Hydra.Utils;

import javafx.animation.FadeTransition;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class GUITextUtils {

    public static void animateText(Label label, String text){
        FadeTransition ft = new FadeTransition(Duration.millis(500), label);
        ft.setFromValue(1f);
        ft.setToValue(0f);
        ft.setOnFinished((handler) -> {
            label.setText(text);
            FadeTransition t2 = new FadeTransition(Duration.millis(500), label);
            t2.setFromValue(0f);
            t2.setToValue(1f);
            t2.play();
        });
        ft.play();
    }

    public static void animateTextAndColor(Label label, String text, String colorHex){
        FadeTransition ft = new FadeTransition(Duration.millis(500), label);
        ft.setFromValue(1f);
        ft.setToValue(0f);
        ft.setOnFinished((handler) -> {
            label.setText(text);
            label.setTextFill(Color.web(colorHex));
            FadeTransition t2 = new FadeTransition(Duration.millis(500), label);
            t2.setFromValue(0f);
            t2.setToValue(1f);
            t2.play();
        });
        ft.play();
    }
}
