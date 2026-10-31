package com.agustin.pillscheck.data;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.agustin.pillscheck.data.dao.IntakeRecordDao;
import com.agustin.pillscheck.data.dao.MedicationDao;
import com.agustin.pillscheck.data.entity.IntakeRecordEntity;
import com.agustin.pillscheck.data.entity.MedicationEntity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;

@Database(entities = {MedicationEntity.class, IntakeRecordEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String TAG = "AppDatabase";
    private static volatile AppDatabase INSTANCE;

    public abstract MedicationDao medicationDao();
    public abstract IntakeRecordDao intakeRecordDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "pills_check.db"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    public static synchronized void seedFromAssets(Context context, MedicationDao dao) {
        try {
            dao.removeDuplicateKeys();

            InputStream is = null;
            try {
                // First attempt: local_seed.json (local personal seed)
                is = context.getAssets().open("local_seed.json");
                Log.i(TAG, "Loading personal seed from local_seed.json");
            } catch (Exception e) {
                // Fallback: default_medications.json (public clean template)
                is = context.getAssets().open("default_medications.json");
                Log.i(TAG, "Loading generic seed from default_medications.json");
            }

            if (is != null) {
                Gson gson = new Gson();
                Type listType = new TypeToken<List<MedicationEntity>>() {}.getType();
                List<MedicationEntity> list = gson.fromJson(new InputStreamReader(is), listType);
                if (list != null && !list.isEmpty()) {
                    for (MedicationEntity med : list) {
                        MedicationEntity existing = dao.getMedicationByKey(med.getKey());
                        if (existing == null) {
                            dao.insert(med);
                        }
                    }
                }
                is.close();
            }

            dao.removeDuplicateKeys();
        } catch (Exception e) {
            Log.e(TAG, "Error seeding medications from assets", e);
        }
    }
}
