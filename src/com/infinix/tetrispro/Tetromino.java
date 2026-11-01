package com.infinix.tetrispro;

import android.graphics.Color;

public class Tetromino {
    public final String name;
    public int[][] shape;
    public final int primaryColor;
    public final int highlightColor;

    public Tetromino(String name, int[][] shape, int primary, int highlight) {
        this.name = name;
        this.shape = shape;
        this.primaryColor = primary;
        this.highlightColor = highlight;
    }

    public Tetromino clonePiece() {
        int rows = shape.length;
        int cols = shape[0].length;
        int[][] s = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            System.arraycopy(shape[r], 0, s[r], 0, cols);
        }
        return new Tetromino(name, s, primaryColor, highlightColor);
    }

    public static Tetromino create(String type) {
        switch (type) {
            case "I":
                return new Tetromino("I", new int[][]{
                    {0,0,0,0},
                    {1,1,1,1},
                    {0,0,0,0},
                    {0,0,0,0}
                }, Color.parseColor("#00E5FF"), Color.parseColor("#B2F5FF"));
            case "O":
                return new Tetromino("O", new int[][]{
                    {1,1},
                    {1,1}
                }, Color.parseColor("#FFD600"), Color.parseColor("#FFF3B0"));
            case "T":
                return new Tetromino("T", new int[][]{
                    {0,1,0},
                    {1,1,1},
                    {0,0,0}
                }, Color.parseColor("#A855F7"), Color.parseColor("#E9D5FF"));
            case "S":
                return new Tetromino("S", new int[][]{
                    {0,1,1},
                    {1,1,0},
                    {0,0,0}
                }, Color.parseColor("#10B981"), Color.parseColor("#A7F3D0"));
            case "Z":
                return new Tetromino("Z", new int[][]{
                    {1,1,0},
                    {0,1,1},
                    {0,0,0}
                }, Color.parseColor("#FF3366"), Color.parseColor("#FECDD3"));
            case "J":
                return new Tetromino("J", new int[][]{
                    {1,0,0},
                    {1,1,1},
                    {0,0,0}
                }, Color.parseColor("#3B82F6"), Color.parseColor("#BFDBFE"));
            case "L":
            default:
                return new Tetromino("L", new int[][]{
                    {0,0,1},
                    {1,1,1},
                    {0,0,0}
                }, Color.parseColor("#FF8C00"), Color.parseColor("#FFEDD5"));
        }
    }
}
