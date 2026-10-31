package com.agustin.pillscheck.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.agustin.pillscheck.data.entity.MedicationEntity;

import java.util.List;

@Dao
public interface MedicationDao {

    @Query("SELECT * FROM medications ORDER BY id ASC")
    List<MedicationEntity> getAllMedications();

    @Query("SELECT * FROM medications WHERE `key` = :key LIMIT 1")
    MedicationEntity getMedicationByKey(String key);

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    MedicationEntity getMedicationById(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MedicationEntity med);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<MedicationEntity> meds);

    @Update
    void update(MedicationEntity med);
}
