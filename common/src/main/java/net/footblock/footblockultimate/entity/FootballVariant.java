package net.footblock.footblockultimate.entity;

public enum FootballVariant {
    CLASSIC(0),
    WORLD_CUP_2026(1);

    private final int id;

    FootballVariant(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public static FootballVariant byId(int id) {
        return id == WORLD_CUP_2026.id ? WORLD_CUP_2026 : CLASSIC;
    }
}
