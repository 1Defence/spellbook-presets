package com.example;

import java.util.Map;

public class SpellbookPresetJsonObject {
    public String preset;
    public Map<Integer, Map<Integer, SpellData>> data;

    public SpellbookPresetJsonObject(String preset, Map<Integer, Map<Integer, SpellData>> data) {
        this.preset = preset;
        this.data = data;
    }
}