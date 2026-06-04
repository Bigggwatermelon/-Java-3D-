package com.gymman.fitnessrpg.ui;

import com.gymman.fitnessrpg.model.AvatarProgress;
import com.gymman.fitnessrpg.model.MuscleGroup;
import com.gymman.fitnessrpg.progression.AvatarProgressionService;
import com.gymman.fitnessrpg.progression.LevelChange;
import com.gymman.fitnessrpg.progression.ProgressionResult;
import com.gymman.fitnessrpg.progression.WorkoutSession;
import com.gymman.fitnessrpg.visual.AvatarVisualState;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class FitnessRpgVisualizerPanel extends JPanel {
    private static final Color PANEL_BG = new Color(246, 247, 250);
    private static final Color INK = new Color(31, 41, 55);
    private static final Color MUTED = new Color(88, 99, 117);

    private AvatarProgress progress;
    private final AvatarProgressionService service;
    private final ProceduralAvatarCanvas avatarCanvas;
    private final MuscleStatsPanel statsPanel;
    private final JLabel headlineLabel;
    private final JLabel eventLabel;
    private final JComboBox<XpOption> xpCombo;
    private final JCheckBox autoRotateBox;

    public FitnessRpgVisualizerPanel() {
        super(new BorderLayout(16, 16));
        this.progress = new AvatarProgress();
        this.service = AvatarProgressionService.defaults();
        this.avatarCanvas = new ProceduralAvatarCanvas();
        this.statsPanel = new MuscleStatsPanel();
        this.headlineLabel = new JLabel();
        this.eventLabel = new JLabel();
        this.xpCombo = new JComboBox<>(XpOption.defaults());
        this.autoRotateBox = new JCheckBox("Auto rotate", true);

        setPreferredSize(new Dimension(1180, 760));
        setBackground(PANEL_BG);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        add(buildTopBar(), BorderLayout.NORTH);
        add(avatarCanvas, BorderLayout.CENTER);
        add(buildControlPanel(), BorderLayout.EAST);

        autoRotateBox.addActionListener(event -> avatarCanvas.setAutoRotate(autoRotateBox.isSelected()));
        updateView("Ready");
    }

    public void addXp(MuscleGroup group, long xp) {
        applyWorkout(WorkoutSession.single(group, xp), group.displayName() + " +" + xp + " XP");
    }

    public void applyWorkout(WorkoutSession session, String label) {
        ProgressionResult result = service.logWorkout(progress, session);
        EnumSet<MuscleGroup> changedGroups = session.xpByGroup().entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(MuscleGroup.class)));
        avatarCanvas.flash(changedGroups);
        updateView(formatLevelUps(label, result));
    }

    public void resetProgress() {
        progress = new AvatarProgress();
        avatarCanvas.clearFlashes();
        updateView("Reset");
    }

    public AvatarProgress progress() {
        return progress;
    }

    public AvatarVisualState visualState() {
        return service.calculateVisualState(progress);
    }

    public void dispose() {
        avatarCanvas.dispose();
    }

    private JPanel buildTopBar() {
        JPanel panel = new JPanel(new BorderLayout(12, 4));
        panel.setOpaque(false);

        headlineLabel.setFont(headlineLabel.getFont().deriveFont(Font.BOLD, 20f));
        headlineLabel.setForeground(INK);

        eventLabel.setFont(eventLabel.getFont().deriveFont(Font.PLAIN, 13f));
        eventLabel.setForeground(MUTED);

        panel.add(headlineLabel, BorderLayout.NORTH);
        panel.add(eventLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildControlPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(true);
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 226, 235)),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
        ));
        panel.setPreferredSize(new Dimension(390, 1));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 10, 0);

        JLabel xpLabel = sectionLabel("Manual XP");
        panel.add(xpLabel, gbc);

        gbc.gridy++;
        panel.add(xpCombo, gbc);

        for (MuscleGroup group : MuscleGroup.values()) {
            gbc.gridy++;
            panel.add(xpButton(group), gbc);
        }

        gbc.gridy++;
        gbc.insets = new Insets(14, 0, 10, 0);
        panel.add(sectionLabel("Scenarios"), gbc);

        gbc.insets = new Insets(0, 0, 8, 0);
        gbc.gridy++;
        panel.add(commandButton("Extreme upper body", this::applyUpperBodyScenario), gbc);

        gbc.gridy++;
        panel.add(commandButton("Explode legs/back", this::applyStructuralCatchUpScenario), gbc);

        gbc.gridy++;
        panel.add(commandButton("Balanced full body", this::applyBalancedScenario), gbc);

        gbc.gridy++;
        panel.add(commandButton("Decay pump 6h", () -> {
            progress.decayPump(6.0, 6.0);
            updateView("Pump decayed by 6 hours");
        }), gbc);

        gbc.gridy++;
        panel.add(commandButton("Reset", this::resetProgress), gbc);

        gbc.gridy++;
        gbc.insets = new Insets(14, 0, 8, 0);
        panel.add(sectionLabel("View"), gbc);

        gbc.gridy++;
        autoRotateBox.setOpaque(false);
        autoRotateBox.setSelected(true);
        panel.add(autoRotateBox, gbc);

        gbc.gridy++;
        JSlider yawSlider = new JSlider(SwingConstants.HORIZONTAL, -180, 180, 0);
        yawSlider.setMajorTickSpacing(90);
        yawSlider.setPaintTicks(true);
        yawSlider.setPaintLabels(true);
        yawSlider.addChangeListener(event -> {
            if (yawSlider.getValueIsAdjusting()) {
                autoRotateBox.setSelected(false);
                avatarCanvas.setAutoRotate(false);
            }
            avatarCanvas.setYawDegrees(yawSlider.getValue());
        });
        panel.add(yawSlider, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(16, 0, 0, 0);
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(statsPanel, gbc);

        return panel;
    }

    private JButton xpButton(MuscleGroup group) {
        JButton button = commandButton("+" + group.displayName(), () -> {
            XpOption option = (XpOption) xpCombo.getSelectedItem();
            long xp = option == null ? 50_000L : option.xp();
            addXp(group, xp);
        });
        button.setHorizontalAlignment(SwingConstants.LEFT);
        return button;
    }

    private JButton commandButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.addActionListener(event -> action.run());
        return button;
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(INK);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        return label;
    }

    private void applyUpperBodyScenario() {
        applyWorkout(WorkoutSession.builder()
                .addXp(MuscleGroup.CHEST, 1_250_000)
                .addXp(MuscleGroup.ARMS, 900_000)
                .addXp(MuscleGroup.BACK, 650_000)
                .addXp(MuscleGroup.ABS, 200_000)
                .addXp(MuscleGroup.LEGS, 3_000)
                .build(), "Extreme upper-body scenario");
    }

    private void applyStructuralCatchUpScenario() {
        applyWorkout(WorkoutSession.builder()
                .addXp(MuscleGroup.LEGS, 1_200_000)
                .addXp(MuscleGroup.BACK, 900_000)
                .build(), "Extreme legs/back scenario");
    }

    private void applyBalancedScenario() {
        applyWorkout(WorkoutSession.builder()
                .addXp(MuscleGroup.CHEST, 85_000)
                .addXp(MuscleGroup.ABS, 70_000)
                .addXp(MuscleGroup.ARMS, 80_000)
                .addXp(MuscleGroup.BACK, 90_000)
                .addXp(MuscleGroup.LEGS, 95_000)
                .build(), "Balanced workout");
    }

    private void updateView(String eventText) {
        AvatarVisualState visual = service.calculateVisualState(progress);
        avatarCanvas.setVisualState(visual);
        statsPanel.update(progress, service.xpCurve(), visual);
        headlineLabel.setText(String.format(Locale.US,
                "No cap mode   Upper/lower %.2f   Chest %.1fx   Legs %.1fx",
                visual.upperLowerVisualRatio(),
                visual.part(MuscleGroup.CHEST).localScale().z(),
                visual.part(MuscleGroup.LEGS).localScale().z()));
        eventLabel.setText(eventText);
    }

    private static String formatLevelUps(String label, ProgressionResult result) {
        String levelUps = result.levelChanges().stream()
                .filter(LevelChange::leveledUp)
                .map(change -> change.group().displayName() + " " + change.beforeLevel() + "->" + change.afterLevel())
                .collect(Collectors.joining(", "));
        if (levelUps.isBlank()) {
            return label;
        }
        return label + "   Level up: " + levelUps;
    }

    private record XpOption(String label, long xp) {
        static XpOption[] defaults() {
            return new XpOption[]{
                    new XpOption("+10,000 XP", 10_000L),
                    new XpOption("+50,000 XP", 50_000L),
                    new XpOption("+250,000 XP", 250_000L),
                    new XpOption("+1,000,000 XP", 1_000_000L),
                    new XpOption("+5,000,000 XP", 5_000_000L)
            };
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
