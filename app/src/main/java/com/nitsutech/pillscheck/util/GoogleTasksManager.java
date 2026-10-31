package com.nitsutech.pillscheck.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nitsutech.pillscheck.data.AppDatabase;
import com.nitsutech.pillscheck.data.entity.IntakeRecordEntity;
import com.nitsutech.pillscheck.data.entity.MedicationEntity;
import com.nitsutech.pillscheck.receiver.NotificationActionReceiver;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoogleTasksManager {

    private static final String TAG = "GoogleTasksManager";
    public static final int RC_SIGN_IN = 9001;
    public static final String TASKS_SCOPE = "https://www.googleapis.com/auth/tasks";
    private static final String TASKS_BASE_URL = "https://tasks.googleapis.com/tasks/v1";

    private static GoogleTasksManager instance;
    private final Context context;
    private final PreferenceHelper prefs;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public interface SyncCallback {
        void onSyncComplete(boolean success, String message);
    }

    private GoogleTasksManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = new PreferenceHelper(this.context);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized GoogleTasksManager getInstance(Context context) {
        if (instance == null) {
            instance = new GoogleTasksManager(context);
        }
        return instance;
    }

    public GoogleSignInClient getGoogleSignInClient(Activity activity) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(TASKS_SCOPE))
                .build();
        return GoogleSignIn.getClient(activity, gso);
    }

    public boolean handleSignInResult(Intent data) {
        try {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null && account.getEmail() != null) {
                prefs.setGoogleTasksAccount(account.getEmail());
                prefs.setGoogleTasksEnabled(true);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Sign in failed", e);
        }
        return false;
    }

    public void signOut(Activity activity, Runnable onComplete) {
        GoogleSignInClient client = getGoogleSignInClient(activity);
        client.signOut().addOnCompleteListener(activity, task -> {
            prefs.setGoogleTasksAccount("");
            prefs.setGoogleTasksEnabled(false);
            if (onComplete != null) onComplete.run();
        });
    }

    public boolean isConnected() {
        return prefs.isGoogleTasksEnabled() && !prefs.getGoogleTasksAccount().isEmpty();
    }

    public String getConnectedAccountEmail() {
        return prefs.getGoogleTasksAccount();
    }

    private String getAuthToken() {
        try {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
            if (account == null || account.getAccount() == null) {
                return null;
            }
            return GoogleAuthUtil.getToken(context, account.getAccount(), "oauth2:" + TASKS_SCOPE);
        } catch (Exception e) {
            Log.e(TAG, "Error obtaining auth token", e);
            return null;
        }
    }

    public static class TaskItem {
        public String id;
        public String title;
        public String status;
        public String completed;
        public String updated;
    }

    public List<TaskItem> fetchTasks(String token) {
        List<TaskItem> list = new ArrayList<>();
        if (token == null) return list;

        HttpURLConnection conn = null;
        try {
            URL url = new URL(TASKS_BASE_URL + "/lists/@default/tasks?showCompleted=true&showHidden=true&maxResults=100");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JsonObject json = new Gson().fromJson(sb.toString(), JsonObject.class);
                if (json.has("items")) {
                    JsonArray items = json.getAsJsonArray("items");
                    for (JsonElement el : items) {
                        JsonObject obj = el.getAsJsonObject();
                        TaskItem item = new TaskItem();
                        item.id = obj.has("id") ? obj.get("id").getAsString() : "";
                        item.title = obj.has("title") ? obj.get("title").getAsString() : "";
                        item.status = obj.has("status") ? obj.get("status").getAsString() : "";
                        item.completed = obj.has("completed") ? obj.get("completed").getAsString() : "";
                        item.updated = obj.has("updated") ? obj.get("updated").getAsString() : "";
                        list.add(item);
                    }
                }
            } else {
                Log.w(TAG, "fetchTasks failed with code: " + responseCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "fetchTasks exception", e);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return list;
    }

    public boolean completeTask(String token, String taskId) {
        if (token == null || taskId == null || taskId.isEmpty()) return false;

        HttpURLConnection conn = null;
        try {
            URL url = new URL(TASKS_BASE_URL + "/lists/@default/tasks/" + taskId);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            JsonObject body = new JsonObject();
            body.addProperty("status", "completed");

            byte[] outputBytes = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(outputBytes);
                os.flush();
            }

            int code = conn.getResponseCode();
            return code >= 200 && code < 300;
        } catch (Exception e) {
            Log.e(TAG, "completeTask exception", e);
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    public void onMedicationTaken(MedicationEntity med) {
        if (!isConnected()) return;

        executor.execute(() -> {
            String token = getAuthToken();
            if (token == null) return;

            String mode = prefs.getGoogleTasksMode();
            List<TaskItem> tasks = fetchTasks(token);

            if (PreferenceHelper.SYNC_MODE_SINGLE.equals(mode)) {
                // If in single mode, verify if all medications are now taken
                AppDatabase db = AppDatabase.getInstance(context);
                List<MedicationEntity> allMeds = db.medicationDao().getAllMedications();
                boolean allTaken = true;
                for (MedicationEntity m : allMeds) {
                    if (m.getActivePillsCount() >= m.getYesterdayCount()) {
                        allTaken = false;
                        break;
                    }
                }
                if (allTaken) {
                    String taskTitle = prefs.getGlobalGoogleTaskName();
                    for (TaskItem item : tasks) {
                        if (item.title != null && item.title.trim().equalsIgnoreCase(taskTitle.trim())
                                && !"completed".equalsIgnoreCase(item.status)) {
                            completeTask(token, item.id);
                            break;
                        }
                    }
                }
            } else {
                // Individual mode: complete the specific task for this medication
                String taskTitle = prefs.getMedicationGoogleTaskName(med.getKey(), med.getName());
                for (TaskItem item : tasks) {
                    if (item.title != null && item.title.trim().equalsIgnoreCase(taskTitle.trim())
                            && !"completed".equalsIgnoreCase(item.status)) {
                        completeTask(token, item.id);
                        break;
                    }
                }
            }
        });
    }

    public void onAllMedicationsTaken() {
        if (!isConnected()) return;

        executor.execute(() -> {
            String token = getAuthToken();
            if (token == null) return;

            String mode = prefs.getGoogleTasksMode();
            List<TaskItem> tasks = fetchTasks(token);

            if (PreferenceHelper.SYNC_MODE_SINGLE.equals(mode)) {
                String taskTitle = prefs.getGlobalGoogleTaskName();
                for (TaskItem item : tasks) {
                    if (item.title != null && item.title.trim().equalsIgnoreCase(taskTitle.trim())
                            && !"completed".equalsIgnoreCase(item.status)) {
                        completeTask(token, item.id);
                        break;
                    }
                }
            } else {
                AppDatabase db = AppDatabase.getInstance(context);
                List<MedicationEntity> allMeds = db.medicationDao().getAllMedications();
                for (MedicationEntity med : allMeds) {
                    String taskTitle = prefs.getMedicationGoogleTaskName(med.getKey(), med.getName());
                    for (TaskItem item : tasks) {
                        if (item.title != null && item.title.trim().equalsIgnoreCase(taskTitle.trim())
                                && !"completed".equalsIgnoreCase(item.status)) {
                            completeTask(token, item.id);
                            break;
                        }
                    }
                }
            }
        });
    }

    public void syncFromGoogleTasks(SyncCallback callback) {
        if (!isConnected()) {
            if (callback != null) callback.onSyncComplete(false, "Not connected");
            return;
        }

        executor.execute(() -> {
            String token = getAuthToken();
            if (token == null) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onSyncComplete(false, "Could not acquire token"));
                }
                return;
            }

            try {
                List<TaskItem> tasks = fetchTasks(token);
                String todayDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
                String mode = prefs.getGoogleTasksMode();
                AppDatabase db = AppDatabase.getInstance(context);
                List<MedicationEntity> meds = db.medicationDao().getAllMedications();
                boolean modifiedLocally = false;

                if (PreferenceHelper.SYNC_MODE_SINGLE.equals(mode)) {
                    String targetTitle = prefs.getGlobalGoogleTaskName();
                    boolean taskCompletedToday = isCompletedToday(tasks, targetTitle, todayDateStr);

                    if (taskCompletedToday && !prefs.isTakenForDate(todayDateStr)) {
                        // Save snapshot for exact undo
                        List<String> slotSnapshots = new ArrayList<>();
                        for (MedicationEntity m : meds) {
                            slotSnapshots.add(m.getKey() + ":::" + m.getActiveSlotsJson());
                        }
                        prefs.setLastIntakeSnapshot(new Gson().toJson(slotSnapshots));
                        // Take 1 pill from each medication
                        int folcresRemaining = 0;
                        int minoxRemaining = 0;
                        int vitDRemaining = 0;

                        for (MedicationEntity med : meds) {
                            if (med.isPendingToday()) {
                                med.consumeOnePill();
                                db.medicationDao().update(med);
                            }
                            int remaining = med.getActivePillsCount();
                            if ("folcres".equals(med.getKey())) folcresRemaining = remaining;
                            else if ("minoxidil".equals(med.getKey())) minoxRemaining = remaining;
                            else if ("vitamind".equals(med.getKey())) vitDRemaining = remaining;
                        }

                        String timeStr = new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
                        IntakeRecordEntity record = new IntakeRecordEntity(
                                System.currentTimeMillis(),
                                todayDateStr,
                                timeStr,
                                folcresRemaining,
                                minoxRemaining,
                                vitDRemaining,
                                "Synced from Google Tasks"
                        );
                        db.intakeRecordDao().insert(record);

                        prefs.setLastIntakeDate(todayDateStr);
                        prefs.setLastIntakeTime(timeStr);
                        modifiedLocally = true;
                    }
                } else {
                    // Individual mode
                    for (MedicationEntity med : meds) {
                        String taskTitle = prefs.getMedicationGoogleTaskName(med.getKey(), med.getName());
                        boolean taskCompletedToday = isCompletedToday(tasks, taskTitle, todayDateStr);

                        if (taskCompletedToday && med.isPendingToday()) {
                            med.consumeOnePill();
                            db.medicationDao().update(med);
                            modifiedLocally = true;
                        }
                    }

                    // Check if all meds are now below yesterday count
                    boolean allTakenNow = true;
                    for (MedicationEntity med : meds) {
                        if (med.getActivePillsCount() >= med.getYesterdayCount()) {
                            allTakenNow = false;
                            break;
                        }
                    }
                    if (allTakenNow && !prefs.isTakenForDate(todayDateStr)) {
                        String timeStr = new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
                        prefs.setLastIntakeDate(todayDateStr);
                        prefs.setLastIntakeTime(timeStr);
                    }
                }

                if (modifiedLocally) {
                    Intent changedIntent = new Intent(NotificationActionReceiver.ACTION_DATA_CHANGED);
                    changedIntent.setPackage(context.getPackageName());
                    context.sendBroadcast(changedIntent);
                }

                if (callback != null) {
                    mainHandler.post(() -> callback.onSyncComplete(true, "OK"));
                }
            } catch (Exception e) {
                Log.e(TAG, "syncFromGoogleTasks error", e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onSyncComplete(false, e.getMessage()));
                }
            }
        });
    }

    private boolean isCompletedToday(List<TaskItem> tasks, String targetTitle, String todayDateStr) {
        if (targetTitle == null || targetTitle.trim().isEmpty()) return false;
        String cleanTarget = targetTitle.trim();

        for (TaskItem item : tasks) {
            if (item.title != null && item.title.trim().equalsIgnoreCase(cleanTarget)) {
                if ("completed".equalsIgnoreCase(item.status)) {
                    // Check completion date or updated date
                    if (item.completed != null && item.completed.contains(todayDateStr)) {
                        return true;
                    }
                    if (item.updated != null && item.updated.contains(todayDateStr)) {
                        return true;
                    }
                    // If completed without exact date match, check if timestamp is within last 18 hours
                    try {
                        SimpleDateFormat rfc3339 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                        String timestamp = item.completed != null ? item.completed : item.updated;
                        if (timestamp != null && timestamp.length() >= 19) {
                            Date date = rfc3339.parse(timestamp.substring(0, 19));
                            if (date != null) {
                                long diff = Math.abs(System.currentTimeMillis() - date.getTime());
                                if (diff < 18 * 60 * 60 * 1000L) {
                                    return true;
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return false;
    }
}
