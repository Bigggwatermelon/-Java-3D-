package com.gymman.fitnessrpg.ui;

import com.gymman.fitnessrpg.model.AvatarProgress;
import com.gymman.fitnessrpg.model.BodyPartProgress;
import com.gymman.fitnessrpg.model.MuscleGroup;
import com.gymman.fitnessrpg.progression.XpCurve;
import com.gymman.fitnessrpg.visual.AvatarVisualState;
import com.gymman.fitnessrpg.visual.MuscleVisualState;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EnumMap;
import java.util.Locale;

final class MuscleStatsPanel extends JPanel {
    private static final Color INK = new Color(31, 41, 55);
    private static final Color MUTED = new Color(88, 99, 117);
    private static final Color EXTREME = new Color(135, 76, 180);

    private final EnumMap<MuscleGroup, Row> rows = new EnumMap<>(MuscleGroup.class);

    MuscleStatsPanel() {
        super(new GridBagLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 8, 0);

        for (MuscleGroup group : MuscleGroup.values()) {
            Row row = new Row(group);
            rows.put(group, row);
            add(row.panel, gbc);
            gbc.gridy++;
        }

        gbc.weighty = 1.0;
        add(new JPanel(), gbc);
    }

    void update(AvatarProgress progress, XpCurve xpCurve, AvatarVisualState visualState) {
        for (MuscleGroup group : MuscleGroup.values()) {
            BodyPartProgress partProgress = progress.progressOf(group, xpCurve);
            MuscleVisualState visual = visualState.part(group);
            rows.get(group).update(partProgress, visual);
        }
    }

    private static final class Row {
        private final JPanel panel = new JPanel(new GridBagLayout());
        private final JLabel title = new JLabel();
        private final JLabel levels = new JLabel();
        private final JLabel xp = new JLabel();
        private final JLabel cap = new JLabel();
        private final JProgressBar visibleGrowth = new JProgressBar(0, 1000);
        private final JProgressBar definition = new JProgressBar(0, 1000);

        Row(MuscleGroup group) {
            panel.setBackground(new Color(248, 250, 252));
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(225, 230, 238)),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)
            ));

            title.setText(group.displayName());
            title.setForeground(INK);
            title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));

            levels.setForeground(MUTED);
            xp.setForeground(MUTED);
            cap.setFont(cap.getFont().deriveFont(Font.BOLD, 11f));

            visibleGrowth.setStringPainted(true);
            definition.setStringPainted(true);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(title, gbc);

            gbc.gridx = 1;
            gbc.weightx = 0.0;
            panel.add(cap, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridwidth = 2;
            gbc.insets = new Insets(3, 0, 0, 0);
            panel.add(levels, gbc);

            gbc.gridy = 2;
            panel.add(xp, gbc);

            gbc.gridy = 3;
            gbc.insets = new Insets(7, 0, 0, 0);
            panel.add(visibleGrowth, gbc);

            gbc.gridy = 4;
            gbc.insets = new Insets(4, 0, 0, 0);
            panel.add(definition, gbc);
        }

        void update(BodyPartProgress partProgress, MuscleVisualState visual) {
            levels.setText(String.format(Locale.US,
                    "Lv %d   visible %.1f",
                    partProgress.level(),
                    visual.visibleLevel()));
            xp.setText(String.format(Locale.US,
                    "%,d XP   next %,d",
                    partProgress.totalXp(),
                    partProgress.xpForNextLevel()));

            visibleGrowth.setValue((int) Math.round(Math.min(1.0, visual.balancedGrowth01() / 6.0) * 1000.0));
            visibleGrowth.setString(String.format(Locale.US,
                    "bulk power %.0f%%   scale %.1fx/%.1fx",
                    visual.balancedGrowth01() * 100.0,
                    visual.localScale().x(),
                    visual.localScale().z()));
            definition.setValue((int) Math.round(visual.definitionMorphWeight() * 1000.0));
            definition.setString(String.format(Locale.US, "definition %.0f%%", visual.definitionMorphWeight() * 100.0));

            cap.setText("NO CAP");
            cap.setForeground(EXTREME);
        }
    }
}
