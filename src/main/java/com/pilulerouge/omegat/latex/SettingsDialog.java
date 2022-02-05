/**************************************************************************
 Simple LaTeX filter for OmegaT

 Copyright (C) 2022 Lev Abashkin

 This file is NOT a part of OmegaT.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package com.pilulerouge.omegat.latex;


import org.omegat.util.OStrings;
import org.openide.awt.Mnemonics;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle;
import java.awt.Color;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static com.pilulerouge.omegat.latex.Util.RB;


/**
 * Settings dialog in OmegaT.
 */
final class SettingsDialog extends JDialog {

    static final String CONF_LOAD_USER_CONFIG = "loadUserConfig";
    static final String CONF_LATEX_COMMAND_COLOR = "latexCommandColor";
    static final String CONF_CURLY_BRACE_COLOR = "curlyBraceColor";

    static final String DEFAULT_LATEX_COMMAND_COLOR = "#00A517";
    static final String DEFAULT_CURLY_BRACE_COLOR = "#389BCD";

    private Map<String, String> options;

    private JPanel panel;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JLabel commandColorLabel;
    private JLabel curlyBraceColorLabel;
    private JButton chooseCommandColorButton;
    private JButton chooseCurlyBraceColorButton;
    private JCheckBox loadUserConfigCheckBox;

    /**
     * Constructor.
     * @param parent parent window
     * @param options options
     */
    SettingsDialog(final Window parent, final Map<String, String> options) {
        super(parent);
        initComponents();

        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        setMinimumSize(new java.awt.Dimension(530, 200));

        this.options = new TreeMap<>(options);

        // Set localized UI text values
        setTitle(RB.getString("SETTINGS_TITLE"));
        loadUserConfigCheckBox.setText(RB.getString("SETTINGS_USER_CONFIG"));
        curlyBraceColorLabel.setText(RB.getString("SETTINGS_CURLY_BRACE_COLOR_EXAMPLE"));
        commandColorLabel.setText(RB.getString("SETTINGS_LATEX_COMMAND_COLOR_EXAMPLE"));
        chooseCurlyBraceColorButton.setText(RB.getString("SETTINGS_CHOOSE_COLOR_BUTTON"));
        chooseCommandColorButton.setText(RB.getString("SETTINGS_CHOOSE_COLOR_BUTTON"));

        Mnemonics.setLocalizedText(buttonOK, OStrings.getString("BUTTON_OK"));
        Mnemonics.setLocalizedText(buttonCancel, OStrings.getString("BUTTON_CANCEL"));

        // Set values to control elements
        String loadUserConfig = options.getOrDefault(CONF_LOAD_USER_CONFIG, "false");
        loadUserConfigCheckBox.setSelected(Boolean.parseBoolean(loadUserConfig));

        Color extraTagColor = Color.decode(options.getOrDefault(CONF_LATEX_COMMAND_COLOR, DEFAULT_LATEX_COMMAND_COLOR));
        commandColorLabel.setForeground(extraTagColor);
        commandColorLabel.setBackground(Color.WHITE);

        Color extraTextColor = Color.decode(options.getOrDefault(CONF_CURLY_BRACE_COLOR, DEFAULT_CURLY_BRACE_COLOR));
        curlyBraceColorLabel.setForeground(extraTextColor);
        curlyBraceColorLabel.setBackground(Color.WHITE);

        // Set action callbacks
        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        chooseCommandColorButton.addActionListener(e -> {
            Color initialColor = commandColorLabel.getForeground();
            Color newColor = JColorChooser.showDialog(null,
                    RB.getString("SETTINGS_COLOR_DIALOG_TITLE"),
                    initialColor);
            if (newColor != null) {
                commandColorLabel.setForeground(newColor);
            }
        });

        chooseCurlyBraceColorButton.addActionListener(e -> {
            Color initialColor = curlyBraceColorLabel.getForeground();
            Color newColor = JColorChooser.showDialog(null,
                    RB.getString("SETTINGS_COLOR_DIALOG_TITLE"),
                    initialColor);
            if (newColor != null) {
                curlyBraceColorLabel.setForeground(newColor);
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(final WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        panel.registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        setLocationRelativeTo(parent);
    }

    Map<String, String> getOptions() {
        return options;
    }

    private void onOK() {
        boolean copyUserConfig = loadUserConfigCheckBox.isSelected();
        if (copyUserConfig) {
            CommandCenter.copyConfig();
        }
        options.put(CONF_LOAD_USER_CONFIG, Boolean.toString(copyUserConfig));
        options.put(CONF_LATEX_COMMAND_COLOR, colorToHex(commandColorLabel.getForeground()));
        options.put(CONF_CURLY_BRACE_COLOR, colorToHex(curlyBraceColorLabel.getForeground()));
        dispose();
    }

    private void onCancel() {
        options = null;
        dispose();
    }

    /**
     * Convert Color to HEX string.
     * @param color color
     * @return HEX encoded color
     */
    static String colorToHex(final Color color) {
        StringBuilder sb = new StringBuilder();
        sb.append('#');
        Stream.of(color.getRed(), color.getGreen(), color.getBlue())
                .map(v -> {
                            String cs = Integer.toHexString(v).toUpperCase();
                            if (cs.length() == 1) {
                                return "0" + cs;
                            } else {
                                return cs;
                            }
                        }
                ).forEach(sb::append);
        return  sb.toString();
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        panel = new JPanel();
        buttonCancel = new JButton();
        buttonOK = new JButton();
        loadUserConfigCheckBox = new JCheckBox();
        chooseCommandColorButton = new JButton();
        chooseCurlyBraceColorButton = new JButton();
        commandColorLabel = new JLabel();
        curlyBraceColorLabel = new JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(
                layout.createParallelGroup()
                        .addComponent(loadUserConfigCheckBox)
                        .addGroup(
                                layout.createSequentialGroup()
                                        .addComponent(commandColorLabel, GroupLayout.PREFERRED_SIZE,
                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(chooseCommandColorButton)
                        )
                        .addGroup(
                                layout.createSequentialGroup()
                                        .addComponent(curlyBraceColorLabel, GroupLayout.PREFERRED_SIZE,
                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(chooseCurlyBraceColorButton)
                        )
                        .addGroup(
                                layout.createSequentialGroup()
                                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                Short.MAX_VALUE)
                                        .addComponent(buttonOK)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(buttonCancel)
                        )
        );

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(loadUserConfigCheckBox)
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(commandColorLabel)
                                        .addComponent(chooseCommandColorButton)
                        )
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(curlyBraceColorLabel)
                                        .addComponent(chooseCurlyBraceColorButton)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                                30, Short.MAX_VALUE)
                        .addGroup(
                                layout.createParallelGroup()
                                        .addComponent(buttonOK)
                                        .addComponent(buttonCancel)
                        )
        );

        layout.linkSize(buttonOK, buttonCancel);
        layout.linkSize(chooseCommandColorButton, chooseCurlyBraceColorButton);

        setContentPane(panel);
        pack();
    }
}
