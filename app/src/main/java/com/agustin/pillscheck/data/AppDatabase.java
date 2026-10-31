package com.agustin.pillscheck.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

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
import java.util.concurrent.Executors;

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
                    .addCallback(new Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            Executors.newSingleThreadExecutor().execute(() -> {
                                seedFromAssets(context.getApplicationContext(), getInstance(context).medicationDao());
                            });
                        }
                    })
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    public static void seedFromAssets(Context context, MedicationDao dao) {
        try {
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
                    dao.insertAll(list);
                }
                is.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error seeding medications from assets", e);
        }
    }
}
