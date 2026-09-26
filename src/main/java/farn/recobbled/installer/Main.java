package farn.recobbled.installer;

import farn.recobbled.installer.gui.InstallerGui;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Main {

    public static void main(String[] args) {
        try {
            InstallerGui.start();
        } catch (Exception e) {
            SwingUtilities.invokeLater(()->showCrashPane(e));
        }
    }

    private static void showCrashPane(Exception e) {
        StringWriter sw = new StringWriter(800);
        try (PrintWriter pw = new PrintWriter(sw)) {
            e.printStackTrace(pw);
        }

        try {
            String lafCls = UIManager.getSystemLookAndFeelClassName();
            UIManager.setLookAndFeel(lafCls);

            if (lafCls.endsWith("AquaLookAndFeel")) { // patch osx tab text color bug JDK-8251377
                UIManager.put("TabbedPane.foreground", Color.BLACK);
            }
        } catch (Exception ignored) {}

        String st = sw.toString().trim();
        String errorMessage = "An unexpected error occurred!";
        JOptionPane.showMessageDialog(null,st,errorMessage,JOptionPane.ERROR_MESSAGE);
    }

}
