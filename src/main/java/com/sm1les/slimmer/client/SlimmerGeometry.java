package com.sm1les.slimmer.client;

/** Геометрия, сгенерированная из Slimmer.bbmodel (координаты Blockbench, 1 единица = 1 пиксель). */
public final class SlimmerGeometry {
    private SlimmerGeometry() {}

    public static final String[] NAMES = {"leg1", "leg2", "leg3", "leg4", "body", "head"};
    /** x1,y1,z1,x2,y2,z2 */
    public static final float[][] BOX = {
        {1.5f, 0f, -5.5f, 5.5f, 10f, -1.5f},
        {1.5f, 0f, 0.5f, 5.5f, 10f, 4.5f},
        {-4.5f, 0f, 0.75f, -0.5f, 10f, 4.75f},
        {-4.5f, 0f, -5.75f, -0.5f, 10f, -1.75f},
        {-2f, 8f, -3f, 3f, 18f, 2f},
        {-3f, 18f, -6f, 4f, 25f, 5f},
    };
    /** Грани в порядке: north, south, east, west, up, down -> u1,v1,u2,v2 (в сетке 64x64) */
    public static final float[][][] UV = {
        {{24f, 22f, 28f, 32f}, {32f, 22f, 36f, 32f}, {20f, 22f, 24f, 32f}, {28f, 22f, 32f, 32f}, {28f, 22f, 24f, 18f}, {32f, 18f, 28f, 22f}},
        {{24f, 36f, 28f, 46f}, {32f, 36f, 36f, 46f}, {20f, 36f, 24f, 46f}, {28f, 36f, 32f, 46f}, {28f, 36f, 24f, 32f}, {32f, 32f, 28f, 36f}},
        {{4f, 37f, 8f, 47f}, {12f, 37f, 16f, 47f}, {0f, 37f, 4f, 47f}, {8f, 37f, 12f, 47f}, {8f, 37f, 4f, 33f}, {12f, 33f, 8f, 37f}},
        {{40f, 4f, 44f, 14f}, {48f, 4f, 52f, 14f}, {36f, 4f, 40f, 14f}, {44f, 4f, 48f, 14f}, {44f, 4f, 40f, 0f}, {48f, 0f, 44f, 4f}},
        {{5f, 23f, 10f, 33f}, {15f, 23f, 20f, 33f}, {0f, 23f, 5f, 33f}, {10f, 23f, 15f, 33f}, {10f, 23f, 5f, 18f}, {15f, 18f, 10f, 23f}},
        {{11f, 11f, 18f, 18f}, {29f, 11f, 36f, 18f}, {0f, 11f, 11f, 18f}, {18f, 11f, 29f, 18f}, {18f, 11f, 11f, 0f}, {25f, 0f, 18f, 11f}},
    };
    /** Точки вращения частей (центр верха ноги / низ тела / шея) */
    public static final float[][] PIVOT = {
        {3.5f, 10f, -3.5f}, {3.5f, 10f, 2.5f}, {-2.5f, 10f, 2.75f}, {-2.5f, 10f, -3.75f}, {0.5f, 8f, -0.5f}, {0.5f, 18f, -0.5f}
    };
    public static final float CENTER_X = 0.5f;
    public static final float CENTER_Z = -0.5f;
}
