/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package farn.recobbled.installer.gui;

import farn.recobbled.installer.gui.installer.ClientInstaller;
import farn.recobbled.installer.gui.installer.ProfileInstaller;
import farn.recobbled.installer.version.LoaderVersion;
import farn.recobbled.installer.version.RecobbledVersion;
import farn.recobbled.installer.util.Utils;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.DefaultCaret;

public class ClientTab {
	protected static final int HORIZONTAL_SPACING = 4;
	protected static final int VERTICAL_SPACING = 6;

	public JButton buttonInstall;

	public JComboBox<String> gameVersionComboBox;
	private JComboBox<String> loaderVersionComboBox;
	public JTextField installLocation;
	public JButton selectFolderButton;
	public JLabel statusLabel;

	private JPanel pane;

	private JCheckBox createProfile;

	public String name() {
		return "client";
	}

	public void install() {
		doInstall();
	}

	private void doInstall() {
		String gameVersion = (String) gameVersionComboBox.getSelectedItem();
		LoaderVersion loaderVersion = LoaderVersion.get((String) loaderVersionComboBox.getSelectedItem());
		if (loaderVersion == null) return;

		System.out.println("Installing");

		new Thread(() -> {
			try {
				updateProgress(new MessageFormat(Utils.BUNDLE.getString("progress.installing")).format(new Object[]{loaderVersion.version}));
				Path mcPath = Paths.get(installLocation.getText());

				if (!Files.exists(mcPath)) {
					throw new RuntimeException(Utils.BUNDLE.getString("progress.exception.no.launcher.directory"));
				}

				final ProfileInstaller profileInstaller = new ProfileInstaller(mcPath);
				ProfileInstaller.LauncherType launcherType = null;

				if (createProfile.isSelected()) {
					List<ProfileInstaller.LauncherType> types = profileInstaller.getInstalledLauncherTypes();

					if (types.isEmpty()) {
						throw new RuntimeException(Utils.BUNDLE.getString("progress.exception.no.launcher.profile"));
					} else if (types.size() == 1) {
						launcherType = types.get(0);
					} else {
						launcherType = showLauncherTypeSelection();

						if (launcherType == null) {
							// canceled
							statusLabel.setText(Utils.BUNDLE.getString("prompt.ready.install"));
							return;
						}
					}
				}

				String profileName = ClientInstaller.install(mcPath, gameVersion, loaderVersion, this);

				if (createProfile.isSelected()) {
					if (launcherType == null) {
						throw new RuntimeException(Utils.BUNDLE.getString("progress.exception.no.launcher.profile"));
					}

					profileInstaller.setupProfile(profileName, gameVersion, launcherType);
				}

				SwingUtilities.invokeLater(() -> showInstalledMessage(loaderVersion.version, gameVersion, mcPath.resolve("mods")));
			} catch (Exception e) {
				error(e);
			} finally {
				buttonInstall.setEnabled(true);
			}
		}).start();
	}

	private ProfileInstaller.LauncherType showLauncherTypeSelection() {
		Object[] options = {Utils.BUNDLE.getString("prompt.launcher.type.xbox"), Utils.BUNDLE.getString("prompt.launcher.type.win32")};

		int result = JOptionPane.showOptionDialog(null,
				Utils.BUNDLE.getString("prompt.launcher.type.body"),
				Utils.BUNDLE.getString("installer.title"),
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				options,
				options[0]
		);

		if (result == JOptionPane.CLOSED_OPTION) {
			return null;
		}

		return result == JOptionPane.YES_OPTION ? ProfileInstaller.LauncherType.MICROSOFT_STORE : ProfileInstaller.LauncherType.WIN32;
	}

	public JPanel makePanel() {
		pane = new JPanel(new GridBagLayout());
		pane.setBorder(new EmptyBorder(4, 4, 4, 4));

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(VERTICAL_SPACING, HORIZONTAL_SPACING, VERTICAL_SPACING, HORIZONTAL_SPACING);
		c.gridx = c.gridy = 0;

		addRow(pane, c, "prompt.game.version", gameVersionComboBox = new JComboBox<>(), createSpacer());

		addRow(pane, c, "prompt.loader.version",
				loaderVersionComboBox = new JComboBox<>());

		addRow(pane, c, "prompt.select.location",
				installLocation = new JTextField(20),
				selectFolderButton = new JButton());
		selectFolderButton.setText("...");
		selectFolderButton.setPreferredSize(new Dimension(installLocation.getPreferredSize().height, installLocation.getPreferredSize().height));
		selectFolderButton.addActionListener(e -> InstallerGui.selectInstallLocation(() -> installLocation.getText(), s -> installLocation.setText(s)));

		addRow(pane, c, null,
				createProfile = new JCheckBox(Utils.BUNDLE.getString("option.create.profile"), true));

		installLocation.setText(Utils.findDefaultInstallDir().toString());

		addRow(pane, c, null,
				statusLabel = new JLabel());
		statusLabel.setText(Utils.BUNDLE.getString("prompt.ready.install"));

		addLastRow(pane, c, null,
				buttonInstall = new JButton(Utils.BUNDLE.getString("prompt.install")));
		buttonInstall.addActionListener(e -> {
			buttonInstall.setEnabled(false);
			install();
		});

		LoaderVersion.verToLink.keySet().forEach(ver -> {
			loaderVersionComboBox.addItem(ver);
		});
		RecobbledVersion.versions.descendingKeySet().forEach(version -> {
			gameVersionComboBox.addItem(version);
		});

		return pane;
	}

	public void updateProgress(String text) {
		statusLabel.setText(text);
		statusLabel.setForeground(UIManager.getColor("Label.foreground"));
	}

	protected String buildEditorPaneStyle() {
		JLabel label = new JLabel();
		Font font = label.getFont();
		Color color = label.getBackground();
		return String.format(Locale.ENGLISH,
				"font-family:%s;font-weight:%s;font-size:%dpt;background-color: rgb(%d,%d,%d);",
				font.getFamily(), (font.isBold() ? "bold" : "normal"), font.getSize(), color.getRed(), color.getGreen(), color.getBlue()
				);
	}

	public void error(Throwable throwable) {
		StringWriter sw = new StringWriter(800);

		try (PrintWriter pw = new PrintWriter(sw)) {
			throwable.printStackTrace(pw);
		}

		String st = sw.toString().trim();
		System.err.println(st);

		String html = String.format("<html><body style=\"%s\">%s</body></html>",
				buildEditorPaneStyle(),
				st.replace(System.lineSeparator(), "<br>").replace("\t", "&ensp;"));
		JEditorPane textPane = new JEditorPane("text/html", html);
		textPane.setEditable(false);

		statusLabel.setText(throwable.getLocalizedMessage());
		statusLabel.setForeground(Color.RED);

		JOptionPane.showMessageDialog(pane,
				textPane,
				Utils.BUNDLE.getString("prompt.exception.occurrence"),
				JOptionPane.ERROR_MESSAGE);
	}

	protected void addRow(Container parent, GridBagConstraints c, String label, Component... components) {
		addRow(parent, c, false, label, components);
	}

	protected void addLastRow(Container parent, GridBagConstraints c, String label, Component... components) {
		addRow(parent, c, true, label, components);
	}

	protected static Component createSpacer() {
		return Box.createRigidArea(new Dimension(4, 0));
	}

	private void addRow(Container parent, GridBagConstraints c, boolean last, String label, Component... components) {
		if (label != null) {
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.LINE_END;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0;
			parent.add(new JLabel(Utils.BUNDLE.getString(label)), c);
			c.gridx++;
			c.anchor = GridBagConstraints.LINE_START;
			c.fill = GridBagConstraints.HORIZONTAL;
		} else {
			c.gridwidth = 2;
			if (last) c.weighty = 1;
			c.anchor = last ? GridBagConstraints.PAGE_START : GridBagConstraints.CENTER;
			c.fill = GridBagConstraints.NONE;
		}

		c.weightx = 1;

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

		for (Component comp : components) {
			panel.add(comp);
		}

		parent.add(panel, c);

		c.gridy++;
		c.gridx = 0;
	}

	private void showInstalledMessage(String loaderVersion, String gameVersion, Path modsDirectory) {
		JEditorPane pane = new JEditorPane("text/html", "<html><body style=\"" + buildEditorPaneStyle() + "\">" + new MessageFormat(Utils.BUNDLE.getString("prompt.install.successful")).format(new Object[]{loaderVersion, gameVersion}) + "</body></html>");
		pane.setBackground(new Color(0, 0, 0, 0));
		pane.setEditable(false);
		pane.setCaret(new DefaultCaret(){
			@Override
			public int getDot() {
				return 0;
			}

			@Override
			public int getMark() {
				return 0;
			}

			@Override
			public void setSelectionVisible(boolean vis) {
			}

			@Override
			protected void positionCaret(MouseEvent e) {
			}

			@Override
			protected void moveCaret(MouseEvent e) {
			}

		});

		pane.addHyperlinkListener(e -> {
			try {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					if (e.getDescription().equals("fabric://mods")) {
						Desktop.getDesktop().open(modsDirectory.toFile());
					} else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
						Desktop.getDesktop().browse(e.getURL().toURI());
					} else {
						throw new UnsupportedOperationException("Failed to open " + e.getURL().toString());
					}
				}
			} catch (Throwable throwable) {
				error(throwable);
			}
		});

		final Image iconImage = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemClassLoader().getResource("icon.png"));
		JOptionPane.showMessageDialog(
				null,
				pane,
				Utils.BUNDLE.getString("prompt.install.successful.title"),
				JOptionPane.INFORMATION_MESSAGE,
				new ImageIcon(iconImage.getScaledInstance(64, 64, Image.SCALE_DEFAULT))
		);
	}

}
