package com.agustin.pillscheck.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "medications")
public class MedicationEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String key;
    private String name;
    private String subtitle;
    private int rows;
    private int cols;
    private int blisterCapacity;
    private String activeSlotsJson;
    private int reserveBlisters;
    private int reserveLoosePills;
    private int standardBoxPills;
    private String blisterType;
    private int yesterdayCount;
    private int dailyDose;

    public MedicationEntity() {
    }

    @Ignore
    public MedicationEntity(String key, String name, String subtitle, int rows, int cols,
                            int blisterCapacity, String activeSlotsJson, int reserveBlisters,
                            int reserveLoosePills, int standardBoxPills, String blisterType,
                            int yesterdayCount, int dailyDose) {
        this.key = key;
        this.name = name;
        this.subtitle = subtitle;
        this.rows = rows;
        this.cols = cols;
        this.blisterCapacity = blisterCapacity;
        this.activeSlotsJson = activeSlotsJson;
        this.reserveBlisters = reserveBlisters;
        this.reserveLoosePills = reserveLoosePills;
        this.standardBoxPills = standardBoxPills;
        this.blisterType = blisterType;
        this.yesterdayCount = yesterdayCount;
        this.dailyDose = dailyDose > 0 ? dailyDose : 1;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public int getCols() {
        return cols;
    }

    public void setCols(int cols) {
        this.cols = cols;
    }

    public int getBlisterCapacity() {
        return blisterCapacity;
    }

    public void setBlisterCapacity(int blisterCapacity) {
        this.blisterCapacity = blisterCapacity;
    }

    public String getActiveSlotsJson() {
        return activeSlotsJson;
    }

    public void setActiveSlotsJson(String activeSlotsJson) {
        this.activeSlotsJson = activeSlotsJson;
    }

    public int getReserveBlisters() {
        return reserveBlisters;
    }

    public void setReserveBlisters(int reserveBlisters) {
        this.reserveBlisters = reserveBlisters;
    }

    public int getReserveLoosePills() {
        return reserveLoosePills;
    }

    public void setReserveLoosePills(int reserveLoosePills) {
        this.reserveLoosePills = reserveLoosePills;
    }

    public int getStandardBoxPills() {
        return standardBoxPills;
    }

    public void setStandardBoxPills(int standardBoxPills) {
        this.standardBoxPills = standardBoxPills;
    }

    public String getBlisterType() {
        return blisterType;
    }

    public void setBlisterType(String blisterType) {
        this.blisterType = blisterType;
    }

    public int getYesterdayCount() {
        return yesterdayCount;
    }

    public void setYesterdayCount(int yesterdayCount) {
        this.yesterdayCount = yesterdayCount;
    }

    public int getDailyDose() {
        return dailyDose;
    }

    public void setDailyDose(int dailyDose) {
        this.dailyDose = dailyDose;
    }

    public boolean[] getSlotsArray() {
        if (activeSlotsJson == null || activeSlotsJson.trim().isEmpty()) {
            return new boolean[blisterCapacity];
        }
        try {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Boolean>>() {}.getType();
            List<Boolean> list = gson.fromJson(activeSlotsJson, listType);
            if (list == null) return new boolean[blisterCapacity];
            boolean[] array = new boolean[blisterCapacity];
            for (int i = 0; i < Math.min(array.length, list.size()); i++) {
                array[i] = Boolean.TRUE.equals(list.get(i));
            }
            return array;
        } catch (Exception e) {
            return new boolean[blisterCapacity];
        }
    }

    public void setSlotsArray(boolean[] array) {
        if (array == null) {
            this.activeSlotsJson = "[]";
            return;
        }
        List<Boolean> list = new ArrayList<>(array.length);
        for (boolean b : array) {
            list.add(b);
        }
        this.activeSlotsJson = new Gson().toJson(list);
    }

    public int getActivePillsCount() {
        boolean[] slots = getSlotsArray();
        int count = 0;
        for (boolean b : slots) {
            if (b) count++;
        }
        return count;
    }

    public int getTotalPills() {
        return getActivePillsCount() + (reserveBlisters * blisterCapacity) + reserveLoosePills;
    }

    public int getTotalDaysSupply() {
        int dose = dailyDose > 0 ? dailyDose : 1;
        return getTotalPills() / dose;
    }
}
