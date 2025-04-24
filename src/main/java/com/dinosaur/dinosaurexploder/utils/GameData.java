package com.dinosaur.dinosaurexploder.utils;

import com.dinosaur.dinosaurexploder.exception.LockedShipException;

public class GameData {
    // Static variable that stores the selected ship
    private static int selectedShip = 1; // Default ship
    private static ShipUnlockChecker unlockChecker = new ShipUnlockChecker();

    // Static variable that stores the high score
    private static int highScore;

    // Getter and setter for the selected ship
    public static int getSelectedShip() {
        return selectedShip;
    }

    public static void setSelectedShip(int shipNumber) {
        selectedShip = unlockChecker.check(shipNumber);
    }

    public static boolean checkUnlockedShip(int shipNumber) {
        try {
            unlockChecker.check(shipNumber);
            return true;
        } catch (LockedShipException e) {
            return false;
        }
    }

    // Getter for the high score
    public static int getHighScore() {
        highScore = unlockChecker.getHighScore().getHigh();
        return highScore;
    }
}
