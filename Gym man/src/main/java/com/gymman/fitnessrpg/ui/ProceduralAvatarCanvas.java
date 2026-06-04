package com.gymman.fitnessrpg.ui;

import com.gymman.fitnessrpg.model.MuscleGroup;
import com.gymman.fitnessrpg.visual.AvatarVisualState;
import com.gymman.fitnessrpg.visual.MaterialVisualState;
import com.gymman.fitnessrpg.visual.MuscleVisualState;
import com.gymman.fitnessrpg.visual.Scale3;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

final class ProceduralAvatarCanvas extends JPanel {
    private static final long TRANSITION_NANOS = 520_000_000L;
    private static final long FLASH_NANOS = 750_000_000L;
    private static final double LAYOUT_MARGIN = 6.0;
    private static final int MAX_TEXTURE_SIDE = 540;

    private final List<BodyPrimitive> primitives = buildPrimitives();
    private final EnumMap<MuscleGroup, Long> flashStartNanos = new EnumMap<>(MuscleGroup.class);
    private final Timer animationTimer;

    private AvatarVisualState startState;
    private AvatarVisualState targetState;
    private long transitionStartNanos;
    private double yawRadians;
    private boolean autoRotate = true;

    ProceduralAvatarCanvas() {
        setBackground(new Color(17, 24, 39));
        setPreferredSize(new Dimension(700, 680));
        this.animationTimer = new Timer(16, event -> {
            if (autoRotate) {
                yawRadians += 0.0065;
            }
            repaint();
        });
        animationTimer.start();
    }

    void setVisualState(AvatarVisualState visualState) {
        this.startState = targetState == null ? visualState : targetState;
        this.targetState = visualState;
        this.transitionStartNanos = System.nanoTime();
        repaint();
    }

    void setYawDegrees(int degrees) {
        this.yawRadians = Math.toRadians(degrees);
        repaint();
    }

    void setAutoRotate(boolean autoRotate) {
        this.autoRotate = autoRotate;
    }

    void flash(EnumSet<MuscleGroup> groups) {
        long now = System.nanoTime();
        for (MuscleGroup group : groups) {
            flashStartNanos.put(group, now);
        }
    }

    void clearFlashes() {
        flashStartNanos.clear();
    }

    void dispose() {
        animationTimer.stop();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            paintBackground(g);
            if (targetState != null) {
                paintAvatar(g);
            }
        } finally {
            g.dispose();
        }
    }

    private void paintBackground(Graphics2D g) {
        int w = getWidth();
        int h = getHeight();
        g.setPaint(new GradientPaint(0, 0, new Color(24, 34, 52), 0, h, new Color(9, 14, 25)));
        g.fillRect(0, 0, w, h);

        g.setColor(new Color(255, 255, 255, 22));
        int floorY = (int) (h * 0.84);
        for (int i = -8; i <= 8; i++) {
            int x = w / 2 + i * 45;
            g.drawLine(x, floorY, w / 2 + i * 90, h);
        }
        for (int i = 0; i < 7; i++) {
            int y = floorY + i * 24;
            g.drawLine(60, y, w - 60, y);
        }

        g.setColor(new Color(255, 255, 255, 165));
        g.setFont(g.getFont().deriveFont(Font.BOLD, 15f));
        g.drawString("Extreme procedural muscle sandbox", 24, 34);
    }

    private void paintAvatar(Graphics2D g) {
        long now = System.nanoTime();
        double transition = smoothstep(clamp01((double) (now - transitionStartNanos) / TRANSITION_NANOS));
        EnumMap<MuscleGroup, RenderPartState> states = renderStates(transition);
        List<LaidOutPrimitive> layout = solveLayout(states);

        double modelScale = fitScale(layout);
        double centerX = getWidth() * 0.50;
        double centerY = getHeight() * 0.50 + 10.0 * modelScale;

        List<ProjectedPrimitive> projected = new ArrayList<>();
        for (LaidOutPrimitive primitive : layout) {
            projected.add(project(primitive, modelScale, centerX, centerY));
        }
        projected.sort(Comparator.comparingDouble(ProjectedPrimitive::depth));

        paintGroundShadow(g, projected);
        for (ProjectedPrimitive primitive : projected) {
            paintPrimitive(g, primitive, now);
        }
        paintMetrics(g, states);
    }

    private EnumMap<MuscleGroup, RenderPartState> renderStates(double transition) {
        EnumMap<MuscleGroup, RenderPartState> states = new EnumMap<>(MuscleGroup.class);
        for (MuscleGroup group : MuscleGroup.values()) {
            MuscleVisualState start = startState.part(group);
            MuscleVisualState target = targetState.part(group);
            states.put(group, RenderPartState.lerp(start, target, transition));
        }
        return states;
    }

    private List<LaidOutPrimitive> solveLayout(EnumMap<MuscleGroup, RenderPartState> states) {
        RenderPartState chest = states.get(MuscleGroup.CHEST);
        RenderPartState abs = states.get(MuscleGroup.ABS);
        RenderPartState arms = states.get(MuscleGroup.ARMS);
        RenderPartState back = states.get(MuscleGroup.BACK);
        RenderPartState legs = states.get(MuscleGroup.LEGS);

        Dimensions chestDims = dimensions("chest-left", chest);
        Dimensions absUpperDims = dimensions("abs-upper", abs);
        Dimensions absMidDims = dimensions("abs-mid", abs);
        Dimensions absLowDims = dimensions("abs-low", abs);
        Dimensions backDims = dimensions("back-left", back);
        Dimensions latDims = dimensions("lat-left", back);
        Dimensions upperArmDims = dimensions("upper-arm-left", arms);
        Dimensions forearmDims = dimensions("forearm-left", arms);
        Dimensions thighDims = dimensions("thigh-left", legs);
        Dimensions calfDims = dimensions("calf-left", legs);
        BodyPrimitive pelvisBase = byName("pelvis");
        BodyPrimitive headBase = byName("head");
        BodyPrimitive neckBase = byName("neck");

        double chestY = -104.0;
        double chestHalfW = 31.0 + chestDims.width() * 0.5 + Math.max(0.0, chest.scaleX() - 1.0) * 18.0;
        double backHalfW = 65.0 + Math.max(backDims.width(), latDims.width()) * 0.5;
        double absHalfW = Math.max(absUpperDims.width(), Math.max(absMidDims.width(), absLowDims.width())) * 0.5;
        double torsoHalfW = Math.max(chestHalfW, Math.max(backHalfW, absHalfW));

        double chestBottom = chestY + chestDims.height() * 0.5;
        double absUpperY = Math.max(-42.0, chestBottom + LAYOUT_MARGIN + absUpperDims.height() * 0.5);
        double absMidY = Math.max(6.0, absUpperY + absUpperDims.height() * 0.5 + LAYOUT_MARGIN + absMidDims.height() * 0.5);
        double absLowY = Math.max(52.0, absMidY + absMidDims.height() * 0.5 + LAYOUT_MARGIN + absLowDims.height() * 0.5);

        double pelvisW = Math.max(pelvisBase.width(), Math.max(absHalfW * 1.28, torsoHalfW * 0.54));
        double pelvisH = Math.max(pelvisBase.height(), 56.0 + Math.max(0.0, abs.scaleX() - 1.0) * 9.0);
        double pelvisY = Math.max(92.0, absLowY + absLowDims.height() * 0.5 + LAYOUT_MARGIN + pelvisH * 0.5);

        double shoulderX = torsoHalfW + upperArmDims.width() * 0.42 + 8.0;
        double shoulderY = chestY - chestDims.height() * 0.18;
        double upperArmY = shoulderY + upperArmDims.height() * 0.50;
        double forearmY = upperArmY + upperArmDims.height() * 0.48 + forearmDims.height() * 0.48;
        double handY = forearmY + forearmDims.height() * 0.52 + 18.0;

        double hipEdge = Math.max(pelvisW * 0.44, torsoHalfW * 0.38);
        double thighX = hipEdge + thighDims.width() * 0.22;
        double thighY = pelvisY + pelvisH * 0.42 + thighDims.height() * 0.48;
        double calfY = thighY + thighDims.height() * 0.48 + calfDims.height() * 0.48;
        double footY = calfY + calfDims.height() * 0.52 + 18.0;

        double torsoTop = Math.min(chestY - chestDims.height() * 0.5, -82.0 - backDims.height() * 0.5);
        double neckY = torsoTop - neckBase.height() * 0.42;
        double headY = neckY - neckBase.height() * 0.48 - headBase.height() * 0.46;

        List<LaidOutPrimitive> result = new ArrayList<>();
        add(result, "head", RenderPartState.neutral(), 0.0, headY, 8.0, headBase.width(), headBase.height(), headBase.depth());
        add(result, "neck", RenderPartState.neutral(), 0.0, neckY, 0.0, neckBase.width(), neckBase.height(), neckBase.depth());
        add(result, "pelvis", RenderPartState.neutral(), 0.0, pelvisY, 0.0, pelvisW, pelvisH, pelvisBase.depth());

        addSymmetric(result, "back-left", "back-right", back, 43.0 + Math.max(0.0, back.scaleX() - 1.0) * 20.0,
                -82.0, -34.0, backDims);
        addSymmetric(result, "lat-left", "lat-right", back, Math.max(65.0, torsoHalfW - latDims.width() * 0.45),
                -42.0, -22.0, latDims);
        addSymmetric(result, "chest-left", "chest-right", chest, 31.0 + Math.max(0.0, chest.scaleX() - 1.0) * 18.0,
                chestY, 28.0, chestDims);

        add(result, "abs-upper", abs, 0.0, absUpperY, 28.0, absUpperDims);
        add(result, "abs-mid", abs, 0.0, absMidY, 29.0, absMidDims);
        add(result, "abs-low", abs, 0.0, absLowY, 26.0, absLowDims);

        addSymmetric(result, "upper-arm-left", "upper-arm-right", arms, shoulderX, upperArmY, 15.0, upperArmDims);
        addSymmetric(result, "forearm-left", "forearm-right", arms, shoulderX + Math.signum(shoulderX) * 11.0,
                forearmY, 14.0, forearmDims);
        addSymmetric(result, "hand-left", "hand-right", RenderPartState.neutral(), shoulderX + 12.0,
                handY, 12.0, dimensions("hand-left", RenderPartState.neutral()));

        addSymmetric(result, "thigh-left", "thigh-right", legs, thighX, thighY, 7.0, thighDims);
        addSymmetric(result, "calf-left", "calf-right", legs, thighX + 3.0, calfY, 5.0, calfDims);
        addSymmetric(result, "foot-left", "foot-right", RenderPartState.neutral(), thighX + 6.0,
                footY, 25.0, dimensions("foot-left", RenderPartState.neutral()));

        return result;
    }

    private void addSymmetric(List<LaidOutPrimitive> result,
                              String leftName,
                              String rightName,
                              RenderPartState state,
                              double halfX,
                              double y,
                              double z,
                              Dimensions dimensions) {
        add(result, leftName, state, -halfX, y, z, dimensions);
        add(result, rightName, state, halfX, y, z, dimensions);
    }

    private void add(List<LaidOutPrimitive> result,
                     String name,
                     RenderPartState state,
                     double x,
                     double y,
                     double z,
                     Dimensions dimensions) {
        add(result, name, state, x, y, z, dimensions.width(), dimensions.height(), dimensions.depth());
    }

    private void add(List<LaidOutPrimitive> result,
                     String name,
                     RenderPartState state,
                     double x,
                     double y,
                     double z,
                     double width,
                     double height,
                     double depth) {
        BodyPrimitive base = byName(name);
        result.add(new LaidOutPrimitive(base, state, x, y, z, width, height, depth, base.angleRadians()));
    }

    private Dimensions dimensions(String primitiveName, RenderPartState state) {
        BodyPrimitive primitive = byName(primitiveName);
        return new Dimensions(
                primitive.width() * state.scaleX(),
                primitive.height() * state.scaleY(),
                primitive.depth() * state.scaleZ()
        );
    }

    private BodyPrimitive byName(String name) {
        for (BodyPrimitive primitive : primitives) {
            if (primitive.name().equals(name)) {
                return primitive;
            }
        }
        throw new IllegalArgumentException("Unknown primitive: " + name);
    }

    private double fitScale(List<LaidOutPrimitive> layout) {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double cos = Math.cos(yawRadians);
        double sin = Math.sin(yawRadians);

        for (LaidOutPrimitive primitive : layout) {
            double projectedHalfW = (Math.abs(primitive.width() * cos) + Math.abs(primitive.depth() * sin) * 0.82) * 0.5;
            minX = Math.min(minX, primitive.x() - projectedHalfW);
            maxX = Math.max(maxX, primitive.x() + projectedHalfW);
            minY = Math.min(minY, primitive.y() - primitive.height() * 0.5);
            maxY = Math.max(maxY, primitive.y() + primitive.height() * 0.5);
        }

        double availableW = Math.max(100.0, getWidth() - 84.0);
        double availableH = Math.max(100.0, getHeight() - 104.0);
        double layoutW = Math.max(1.0, maxX - minX);
        double layoutH = Math.max(1.0, maxY - minY);
        return Math.min(availableW / layoutW, availableH / layoutH);
    }

    private ProjectedPrimitive project(LaidOutPrimitive primitive, double modelScale, double centerX, double centerY) {
        double cos = Math.cos(yawRadians);
        double sin = Math.sin(yawRadians);
        double rotatedX = primitive.x() * cos + primitive.z() * sin;
        double rotatedZ = primitive.z() * cos - primitive.x() * sin;
        double perspective = Math.max(0.30, 1.0 + rotatedZ * 0.0018);
        double x = centerX + rotatedX * modelScale * perspective;
        double y = centerY + primitive.y() * modelScale;
        double width = (Math.abs(primitive.width() * cos) + Math.abs(primitive.depth() * sin) * 0.82) * modelScale * perspective;
        double height = primitive.height() * modelScale * perspective;
        double angle = primitive.angleRadians() * Math.signum(cos == 0.0 ? 1.0 : cos);
        return new ProjectedPrimitive(primitive, x, y, rotatedZ, Math.max(3.0, width), Math.max(3.0, height), angle);
    }

    private void paintGroundShadow(Graphics2D g, List<ProjectedPrimitive> projected) {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (ProjectedPrimitive primitive : projected) {
            minX = Math.min(minX, primitive.x() - primitive.width() * 0.45);
            maxX = Math.max(maxX, primitive.x() + primitive.width() * 0.45);
            maxY = Math.max(maxY, primitive.y() + primitive.height() * 0.5);
        }
        double w = Math.max(80.0, (maxX - minX) * 0.78);
        double h = Math.max(20.0, w * 0.16);
        double x = (minX + maxX) * 0.5 - w * 0.5;
        double y = maxY - h * 0.22;
        g.setColor(new Color(0, 0, 0, 95));
        g.fill(new Ellipse2D.Double(x, y, w, h));
    }

    private void paintPrimitive(Graphics2D g, ProjectedPrimitive projected, long now) {
        LaidOutPrimitive laidOut = projected.primitive();
        BodyPrimitive primitive = laidOut.primitive();
        RenderPartState state = laidOut.state();
        double flash = flashAmount(primitive.group(), now);
        Color fill = muscleColor(primitive.baseColor(), state.material(), flash);
        Shape outline = primitive.kind() == PrimitiveKind.CAPSULE
                ? capsule(projected.x(), projected.y(), projected.width(), projected.height(), projected.angleRadians())
                : ellipse(projected.x(), projected.y(), projected.width(), projected.height());

        if (flash > 0.0) {
            g.setColor(new Color(255, 232, 156, (int) Math.round(120.0 * flash)));
            g.setStroke(new BasicStroke((float) (8.0 * flash), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(outline);
        }

        BufferedImage shaded = renderNormalMappedPrimitive(
                primitive.kind(),
                primitive.group(),
                projected.width(),
                projected.height(),
                fill,
                state.material(),
                flash
        );
        drawTexturedShape(g, shaded, projected.x(), projected.y(), projected.width(), projected.height(), projected.angleRadians());

        g.setColor(darken(fill, 0.42));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(outline);

        if ("head".equals(primitive.name())) {
            paintFace(g, projected);
        }
    }

    private BufferedImage renderNormalMappedPrimitive(PrimitiveKind kind,
                                                     MuscleGroup group,
                                                     double targetWidth,
                                                     double targetHeight,
                                                     Color base,
                                                     MaterialVisualState material,
                                                     double flash) {
        int imageW = Math.max(8, Math.min(MAX_TEXTURE_SIDE, (int) Math.round(targetWidth)));
        int imageH = Math.max(8, Math.min(MAX_TEXTURE_SIDE, (int) Math.round(targetHeight)));
        BufferedImage image = new BufferedImage(imageW, imageH, BufferedImage.TYPE_INT_ARGB);
        double definition = clamp01(material.normalBlend01());
        double veinLevel = clamp01(material.vascularity01() + material.pump01() * 0.55);
        double[] light = normalize3(-0.48, -0.58, 0.66);
        double[] view = {0.0, 0.0, 1.0};

        for (int y = 0; y < imageH; y++) {
            double v = ((y + 0.5) / imageH) * 2.0 - 1.0;
            for (int x = 0; x < imageW; x++) {
                double u = ((x + 0.5) / imageW) * 2.0 - 1.0;
                MaskSample mask = mask(kind, u, v, imageW, imageH);
                if (mask.alpha() <= 0.0) {
                    continue;
                }

                double baseNz = Math.sqrt(Math.max(0.0, 1.0 - mask.nx() * mask.nx() - mask.ny() * mask.ny()));
                double h = muscleHeight(group, u, v, definition, veinLevel);
                double hx = muscleHeight(group, u + 0.012, v, definition, veinLevel) - h;
                double hy = muscleHeight(group, u, v + 0.012, definition, veinLevel) - h;
                double strength = 2.6 * definition + 0.8 * material.pump01();
                double[] normal = normalize3(mask.nx() - hx * strength, mask.ny() - hy * strength, baseNz);

                double diffuse = Math.max(0.0, dot(normal, light));
                double rim = Math.pow(Math.max(0.0, 1.0 - dot(normal, view)), 2.2);
                double spec = Math.pow(Math.max(0.0, dot(reflect(light, normal), view)), 26.0) * material.specular01();
                double vein = veinMask(group, u, v) * veinLevel;

                Color local = mix(base, new Color(92, 126, 155), vein * 0.42);
                double shade = 0.43 + diffuse * 0.62 + rim * 0.18 + flash * 0.16;
                int r = clampColor((int) Math.round(local.getRed() * shade + spec * 120.0));
                int gr = clampColor((int) Math.round(local.getGreen() * shade + spec * 110.0));
                int b = clampColor((int) Math.round(local.getBlue() * shade + spec * 130.0));
                int a = clampColor((int) Math.round(mask.alpha() * 255.0));
                image.setRGB(x, y, (a << 24) | (r << 16) | (gr << 8) | b);
            }
        }
        return image;
    }

    private void drawTexturedShape(Graphics2D g,
                                   BufferedImage image,
                                   double centerX,
                                   double centerY,
                                   double width,
                                   double height,
                                   double angleRadians) {
        AffineTransform transform = new AffineTransform();
        transform.translate(centerX, centerY);
        transform.rotate(angleRadians);
        transform.scale(width / image.getWidth(), height / image.getHeight());
        transform.translate(-image.getWidth() / 2.0, -image.getHeight() / 2.0);
        g.drawImage(image, transform, null);
    }

    private void paintFace(Graphics2D g, ProjectedPrimitive projected) {
        if (Math.cos(yawRadians) < -0.08) {
            return;
        }

        double w = projected.width();
        double h = projected.height();
        double faceTurn = Math.sin(yawRadians) * 0.16 * w;
        Graphics2D face = (Graphics2D) g.create();
        try {
            face.translate(projected.x() + faceTurn, projected.y());
            face.rotate(projected.angleRadians());
            face.setColor(new Color(33, 38, 49));
            face.fill(new Ellipse2D.Double(-w * 0.23, -h * 0.12, w * 0.10, h * 0.13));
            face.fill(new Ellipse2D.Double(w * 0.13, -h * 0.12, w * 0.10, h * 0.13));
            face.setColor(new Color(255, 255, 255, 230));
            face.fill(new Ellipse2D.Double(-w * 0.20, -h * 0.10, w * 0.025, h * 0.035));
            face.fill(new Ellipse2D.Double(w * 0.16, -h * 0.10, w * 0.025, h * 0.035));
            face.setColor(new Color(118, 38, 55));
            face.setStroke(new BasicStroke((float) Math.max(2.0, w * 0.035), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            face.draw(new Arc2D.Double(-w * 0.27, -h * 0.03, w * 0.54, h * 0.36, 205, 130, Arc2D.OPEN));
        } finally {
            face.dispose();
        }
    }

    private void paintMetrics(Graphics2D g, EnumMap<MuscleGroup, RenderPartState> states) {
        g.setFont(g.getFont().deriveFont(Font.BOLD, 12f));
        int y = getHeight() - 98;
        g.setColor(new Color(255, 255, 255, 188));
        g.drawString("No proportion cap. Limbs are re-anchored from torso bounds.", 24, y);
        y += 18;
        for (MuscleGroup group : MuscleGroup.values()) {
            RenderPartState state = states.get(group);
            String text = String.format(Locale.US, "%s scale X/Z %.1fx / %.1fx",
                    group.displayName(), state.scaleX(), state.scaleZ());
            g.drawString(text, 24, y);
            y += 16;
        }
    }

    private double flashAmount(MuscleGroup group, long now) {
        if (group == null || !flashStartNanos.containsKey(group)) {
            return 0.0;
        }
        double elapsed = (double) (now - flashStartNanos.get(group)) / FLASH_NANOS;
        if (elapsed >= 1.0) {
            flashStartNanos.remove(group);
            return 0.0;
        }
        return 1.0 - smoothstep(elapsed);
    }

    private static Shape ellipse(double centerX, double centerY, double width, double height) {
        return new Ellipse2D.Double(centerX - width / 2.0, centerY - height / 2.0, width, height);
    }

    private static Shape capsule(double centerX, double centerY, double width, double height, double angleRadians) {
        double arc = Math.min(width, height);
        Shape shape = new RoundRectangle2D.Double(-width / 2.0, -height / 2.0, width, height, arc, arc);
        AffineTransform transform = new AffineTransform();
        transform.translate(centerX, centerY);
        transform.rotate(angleRadians);
        return transform.createTransformedShape(shape);
    }

    private static MaskSample mask(PrimitiveKind kind, double u, double v, int width, int height) {
        if (kind == PrimitiveKind.ELLIPSOID) {
            double r2 = u * u + v * v;
            if (r2 > 1.08) {
                return MaskSample.empty();
            }
            double alpha = clamp01((1.08 - r2) / 0.08);
            return new MaskSample(alpha, u * 0.78, v * 0.78);
        }

        double radiusPixels = Math.min(width, height) * 0.5;
        double halfW = width * 0.5;
        double halfH = height * 0.5;
        double px = u * halfW;
        double py = v * halfH;
        double rectHalfH = Math.max(0.0, halfH - radiusPixels);
        double closestY = clamp(py, -rectHalfH, rectHalfH);
        double dx = px;
        double dy = py - closestY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist > radiusPixels + 1.5) {
            return MaskSample.empty();
        }
        double alpha = clamp01((radiusPixels + 1.5 - dist) / 2.5);
        double nx = radiusPixels == 0.0 ? 0.0 : dx / radiusPixels;
        double ny = radiusPixels == 0.0 ? 0.0 : dy / radiusPixels;
        return new MaskSample(alpha, nx * 0.82, ny * 0.82);
    }

    private static double muscleHeight(MuscleGroup group, double u, double v, double definition, double veinLevel) {
        if (group == null) {
            return 0.012 * Math.sin(u * 6.0) * Math.cos(v * 5.0);
        }

        double fiber = 0.0;
        switch (group) {
            case CHEST -> {
                double split = -groove(Math.abs(u), 0.055, 0.020);
                double lowerArc = ridge(v + 0.20 + 0.20 * Math.abs(u), 0.045);
                double upperFiber = Math.sin((u + Math.signum(u) * 0.25) * 18.0 + v * 7.0) * 0.018;
                fiber = split + lowerArc * 0.7 + upperFiber;
            }
            case ABS -> {
                double center = -groove(Math.abs(u), 0.050, 0.020);
                double rows = 0.0;
                for (double row : new double[]{-0.42, -0.12, 0.18, 0.48}) {
                    rows -= groove(Math.abs(v - row), 0.035, 0.015);
                }
                fiber = center + rows + Math.sin(u * 26.0) * 0.010;
            }
            case ARMS -> {
                double longFiber = Math.sin(v * 18.0 + u * 8.0) * 0.018;
                double bicep = ridge(u * 0.55 + Math.sin(v * 2.5) * 0.18, 0.26) * 0.55;
                fiber = longFiber + bicep;
            }
            case BACK -> {
                double spine = -groove(Math.abs(u), 0.045, 0.020);
                double lat = ridge(Math.abs(u) - (0.35 + v * 0.12), 0.12) * 0.65;
                fiber = spine + lat + Math.sin(u * 16.0 - v * 10.0) * 0.015;
            }
            case LEGS -> {
                double quadSplit = -groove(Math.abs(u - 0.18 * Math.sin(v * 2.0)), 0.070, 0.025);
                double longFiber = Math.sin(v * 22.0 - u * 7.0) * 0.020;
                fiber = quadSplit + longFiber + ridge(u + 0.36, 0.20) * 0.25;
            }
        }

        return fiber * definition + veinMask(group, u, v) * 0.075 * veinLevel;
    }

    private static double veinMask(MuscleGroup group, double u, double v) {
        if (group == null) {
            return 0.0;
        }
        double vein = 0.0;
        switch (group) {
            case CHEST -> {
                vein += veinCurve(u, v, -0.28, -0.10, 0.23, 0.38, 0.035);
                vein += veinCurve(u, v, 0.30, -0.14, -0.24, 0.32, 0.032);
            }
            case ABS -> {
                vein += veinCurve(u, v, -0.22, -0.55, 0.18, 0.95, 0.030);
                vein += veinCurve(u, v, 0.24, -0.35, -0.12, 0.82, 0.026);
            }
            case ARMS -> vein += veinCurve(u, v, -0.20, -0.82, 0.36, 1.48, 0.034);
            case BACK -> {
                vein += veinCurve(u, v, -0.34, -0.42, 0.18, 0.84, 0.030);
                vein += veinCurve(u, v, 0.34, -0.42, -0.18, 0.84, 0.030);
            }
            case LEGS -> vein += veinCurve(u, v, 0.18, -0.82, -0.26, 1.55, 0.034);
        }
        return clamp01(vein);
    }

    private static double veinCurve(double u,
                                    double v,
                                    double startU,
                                    double startV,
                                    double deltaU,
                                    double deltaV,
                                    double width) {
        double t = clamp01(((u - startU) * deltaU + (v - startV) * deltaV) / (deltaU * deltaU + deltaV * deltaV));
        double curveU = startU + deltaU * t + Math.sin(t * Math.PI * 3.0) * 0.035;
        double curveV = startV + deltaV * t;
        double dist = Math.hypot(u - curveU, v - curveV);
        return Math.exp(-(dist * dist) / (width * width));
    }

    private static double ridge(double value, double width) {
        return Math.exp(-(value * value) / Math.max(0.0001, width * width));
    }

    private static double groove(double value, double width, double falloff) {
        double inner = Math.exp(-(value * value) / Math.max(0.0001, width * width));
        double outer = Math.exp(-(value * value) / Math.max(0.0001, (width + falloff) * (width + falloff)));
        return clamp01(inner * 1.15 - outer * 0.35);
    }

    private static Color muscleColor(Color base, MaterialVisualState material, double flash) {
        double redPump = material.pump01() * 0.32;
        double sheen = material.specular01() * 0.08 + flash * 0.16;
        int r = clampColor(base.getRed() + (int) Math.round(74.0 * redPump + 68.0 * sheen));
        int g = clampColor(base.getGreen() + (int) Math.round(20.0 * sheen) - (int) Math.round(16.0 * redPump));
        int b = clampColor(base.getBlue() + (int) Math.round(24.0 * sheen) - (int) Math.round(22.0 * redPump));
        return new Color(r, g, b);
    }

    private static Color darken(Color color, double amount) {
        return mix(color, Color.BLACK, amount);
    }

    private static Color mix(Color a, Color b, double amount) {
        double t = clamp01(amount);
        int r = (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(clampColor(r), clampColor(g), clampColor(bl));
    }

    private static double[] normalize3(double x, double y, double z) {
        double len = Math.sqrt(x * x + y * y + z * z);
        if (len == 0.0) {
            return new double[]{0.0, 0.0, 1.0};
        }
        return new double[]{x / len, y / len, z / len};
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] reflect(double[] incident, double[] normal) {
        double d = dot(incident, normal);
        return normalize3(
                incident[0] - 2.0 * d * normal[0],
                incident[1] - 2.0 * d * normal[1],
                incident[2] - 2.0 * d * normal[2]
        );
    }

    private static int clampColor(int value) {
        return Math.min(255, Math.max(0, value));
    }

    private static double smoothstep(double value) {
        double x = clamp01(value);
        return x * x * (3.0 - 2.0 * x);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp01(double value) {
        return clamp(value, 0.0, 1.0);
    }

    private static List<BodyPrimitive> buildPrimitives() {
        Color skin = new Color(206, 157, 119);
        Color chest = new Color(208, 116, 91);
        Color abs = new Color(207, 149, 94);
        Color arms = new Color(201, 137, 101);
        Color back = new Color(151, 128, 189);
        Color legs = new Color(174, 129, 92);
        Color neutral = new Color(184, 151, 118);

        List<BodyPrimitive> parts = new ArrayList<>();
        parts.add(new BodyPrimitive("head", null, PrimitiveKind.ELLIPSOID, 0, -235, 8, 58, 70, 45, 0, skin));
        parts.add(new BodyPrimitive("neck", null, PrimitiveKind.CAPSULE, 0, -185, 0, 34, 52, 28, 0, neutral));
        parts.add(new BodyPrimitive("pelvis", null, PrimitiveKind.ELLIPSOID, 0, 92, 0, 92, 70, 54, 0, neutral));

        parts.add(new BodyPrimitive("back-left", MuscleGroup.BACK, PrimitiveKind.ELLIPSOID, -43, -79, -28, 66, 150, 46, -0.08, back));
        parts.add(new BodyPrimitive("back-right", MuscleGroup.BACK, PrimitiveKind.ELLIPSOID, 43, -79, -28, 66, 150, 46, 0.08, back));
        parts.add(new BodyPrimitive("lat-left", MuscleGroup.BACK, PrimitiveKind.ELLIPSOID, -65, -40, -18, 44, 126, 40, -0.12, back));
        parts.add(new BodyPrimitive("lat-right", MuscleGroup.BACK, PrimitiveKind.ELLIPSOID, 65, -40, -18, 44, 126, 40, 0.12, back));

        parts.add(new BodyPrimitive("chest-left", MuscleGroup.CHEST, PrimitiveKind.ELLIPSOID, -31, -102, 24, 78, 86, 52, -0.03, chest));
        parts.add(new BodyPrimitive("chest-right", MuscleGroup.CHEST, PrimitiveKind.ELLIPSOID, 31, -102, 24, 78, 86, 52, 0.03, chest));

        parts.add(new BodyPrimitive("abs-upper", MuscleGroup.ABS, PrimitiveKind.ELLIPSOID, 0, -42, 25, 72, 58, 34, 0, abs));
        parts.add(new BodyPrimitive("abs-mid", MuscleGroup.ABS, PrimitiveKind.ELLIPSOID, 0, 6, 26, 66, 54, 32, 0, abs));
        parts.add(new BodyPrimitive("abs-low", MuscleGroup.ABS, PrimitiveKind.ELLIPSOID, 0, 52, 23, 62, 50, 30, 0, abs));

        parts.add(new BodyPrimitive("upper-arm-left", MuscleGroup.ARMS, PrimitiveKind.CAPSULE, -106, -76, 14, 42, 118, 38, -0.22, arms));
        parts.add(new BodyPrimitive("upper-arm-right", MuscleGroup.ARMS, PrimitiveKind.CAPSULE, 106, -76, 14, 42, 118, 38, 0.22, arms));
        parts.add(new BodyPrimitive("forearm-left", MuscleGroup.ARMS, PrimitiveKind.CAPSULE, -126, 27, 13, 35, 112, 32, -0.10, arms));
        parts.add(new BodyPrimitive("forearm-right", MuscleGroup.ARMS, PrimitiveKind.CAPSULE, 126, 27, 13, 35, 112, 32, 0.10, arms));
        parts.add(new BodyPrimitive("hand-left", null, PrimitiveKind.ELLIPSOID, -130, 101, 12, 35, 32, 25, 0, skin));
        parts.add(new BodyPrimitive("hand-right", null, PrimitiveKind.ELLIPSOID, 130, 101, 12, 35, 32, 25, 0, skin));

        parts.add(new BodyPrimitive("thigh-left", MuscleGroup.LEGS, PrimitiveKind.CAPSULE, -35, 166, 7, 52, 150, 42, 0.05, legs));
        parts.add(new BodyPrimitive("thigh-right", MuscleGroup.LEGS, PrimitiveKind.CAPSULE, 35, 166, 7, 52, 150, 42, -0.05, legs));
        parts.add(new BodyPrimitive("calf-left", MuscleGroup.LEGS, PrimitiveKind.CAPSULE, -38, 282, 5, 42, 128, 35, -0.03, legs));
        parts.add(new BodyPrimitive("calf-right", MuscleGroup.LEGS, PrimitiveKind.CAPSULE, 38, 282, 5, 42, 128, 35, 0.03, legs));
        parts.add(new BodyPrimitive("foot-left", null, PrimitiveKind.ELLIPSOID, -45, 360, 25, 66, 30, 44, -0.06, skin));
        parts.add(new BodyPrimitive("foot-right", null, PrimitiveKind.ELLIPSOID, 45, 360, 25, 66, 30, 44, 0.06, skin));
        return List.copyOf(parts);
    }

    private enum PrimitiveKind {
        ELLIPSOID,
        CAPSULE
    }

    private record BodyPrimitive(
            String name,
            MuscleGroup group,
            PrimitiveKind kind,
            double x,
            double y,
            double z,
            double width,
            double height,
            double depth,
            double angleRadians,
            Color baseColor
    ) {
    }

    private record Dimensions(double width, double height, double depth) {
    }

    private record LaidOutPrimitive(
            BodyPrimitive primitive,
            RenderPartState state,
            double x,
            double y,
            double z,
            double width,
            double height,
            double depth,
            double angleRadians
    ) {
    }

    private record ProjectedPrimitive(
            LaidOutPrimitive primitive,
            double x,
            double y,
            double depth,
            double width,
            double height,
            double angleRadians
    ) {
    }

    private record MaskSample(double alpha, double nx, double ny) {
        static MaskSample empty() {
            return new MaskSample(0.0, 0.0, 0.0);
        }
    }

    private record RenderPartState(
            Scale3 scale,
            MaterialVisualState material,
            double definition
    ) {
        static RenderPartState neutral() {
            return new RenderPartState(
                    Scale3.identity(),
                    new MaterialVisualState(0.0, 0.7, 0.15, 0.0, 0.0),
                    0.0
            );
        }

        static RenderPartState lerp(MuscleVisualState start, MuscleVisualState target, double amount) {
            return new RenderPartState(
                    new Scale3(
                            lerp(start.localScale().x(), target.localScale().x(), amount),
                            lerp(start.localScale().y(), target.localScale().y(), amount),
                            lerp(start.localScale().z(), target.localScale().z(), amount)
                    ),
                    new MaterialVisualState(
                            lerp(start.material().normalBlend01(), target.material().normalBlend01(), amount),
                            lerp(start.material().roughness(), target.material().roughness(), amount),
                            lerp(start.material().specular01(), target.material().specular01(), amount),
                            lerp(start.material().vascularity01(), target.material().vascularity01(), amount),
                            lerp(start.material().pump01(), target.material().pump01(), amount)
                    ),
                    lerp(start.definitionMorphWeight(), target.definitionMorphWeight(), amount)
            );
        }

        double scaleX() {
            return scale.x() * (1.0 + material.pump01() * 0.14);
        }

        double scaleY() {
            return scale.y();
        }

        double scaleZ() {
            return scale.z() * (1.0 + material.pump01() * 0.14);
        }

        private static double lerp(double from, double to, double amount) {
            return from + (to - from) * clamp01(amount);
        }
    }
}
