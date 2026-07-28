package com.zaelio.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class BackupJsonRepositoryTest {
    private Context context;
    private TrackingDatabase db;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("tracking.sqlite");
        db = new TrackingDatabase(context);
    }

    @After
    public void tearDown() {
        db.close();
        context.deleteDatabase("tracking.sqlite");
    }

    @Test
    public void exportAllIncludesTrackersSessionsAndRecords() throws Exception {
        Tracker tracker = db.trackers().get(0);
        FieldDefinition field = tracker.fields.get(0);
        Session session = db.session(db.createSession(tracker.id));
        Map<String, Object> values = new HashMap<>();
        values.put(field.key, 15);
        db.saveRecord(session, field.id, values);

        JSONObject export = new JSONObject(BackupJsonRepository.exportAll(db));

        assertEquals("zaelio-backup", export.getString("type"));
        assertEquals(1, export.getJSONArray("trackers").length());
        assertEquals(1, export.getJSONArray("sessions").length());
        assertEquals(1, export.getJSONArray("sessions").getJSONObject(0).getJSONArray("records").length());
    }

    @Test
    public void importAllRecreatesExportedDataWithNewIds() throws Exception {
        Tracker tracker = db.trackers().get(0);
        FieldDefinition field = tracker.fields.get(0);
        Session session = db.session(db.createSession(tracker.id));
        Map<String, Object> values = new HashMap<>();
        values.put(field.key, 20);
        db.saveRecord(session, field.id, values);
        String json = BackupJsonRepository.exportAll(db);

        db.deleteTracker(tracker.id);
        int imported = BackupJsonRepository.importAll(db, json);
        Tracker importedTracker = db.trackers().get(0);
        Session importedSession = db.sessions().get(0);

        assertEquals(1, imported);
        assertEquals("Training", importedTracker.name);
        assertEquals(importedTracker.id, importedSession.trackerId);
        assertEquals(1, db.recordCount(importedSession.id));
        assertTrue(BackupJsonRepository.exportSessions(db).contains("sessions"));
    }

    @Test
    public void trackerOnlyExportSkipsSessions() throws Exception {
        Tracker tracker = db.trackers().get(0);
        db.createSession(tracker.id);

        JSONObject export = new JSONObject(BackupJsonRepository.exportTrackers(db));

        assertEquals(1, export.getJSONArray("trackers").length());
        assertEquals(0, export.getJSONArray("sessions").length());
    }
}
