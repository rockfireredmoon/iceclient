package org.icemoon.domain;

public class ObjectLocation {

    private String terrainTemplate;
    private float worldX;
    private float worldY;

    public ObjectLocation(String terrainTemplate, float worldX, float worldY) {
        this.terrainTemplate = terrainTemplate;
        this.worldX = worldX;
        this.worldY = worldY;
    }

    public String getTerrainTemplate() {
        return terrainTemplate;
    }

    public void setTerrainTemplate(String terrainTemplate) {
        this.terrainTemplate = terrainTemplate;
    }

    public float getWorldX() {
        return worldX;
    }

    public void setWorldX(float worldX) {
        this.worldX = worldX;
    }

    public float getWorldY() {
        return worldY;
    }

    public void setWorldY(float worldY) {
        this.worldY = worldY;
    }
}
