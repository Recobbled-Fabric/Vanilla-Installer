package farn.recobbled.installer;

import farn.recobbled.installer.gui.InstallerGui;

import javax.swing.*;
import javax.xml.stream.XMLStreamException;

public class Main {

    public static void main(String[] args) {
        try {
            InstallerGui.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
