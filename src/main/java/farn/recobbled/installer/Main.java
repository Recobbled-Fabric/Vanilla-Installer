package farn.recobbled.installer;

import farn.recobbled.installer.gui.InstallerGui;

public class Main {

    public static void main(String[] args) {
        try {
            InstallerGui.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
