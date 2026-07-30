package com.zaelio.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TrackingDatabaseTest {
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
    public void seedCreatesTrainingTrackerWithItems() {
        Tracker tracker = db.trackers().get(0);

        assertEquals("Training", tracker.name);
        assertEquals(4, tracker.fields.size());
        assertEquals(4, tracker.items.size());
    }

    @Test
    public void sessionsRecordsPreviousValueAndDeletesWork() {
        Tracker tracker = db.trackers().get(0);
        FieldDefinition reps = tracker.fields.get(0);
        long sessionId = db.createSession(tracker.id);
        Session session = db.session(sessionId);
        Map<String, Object> values = new HashMap<>();
        values.put(reps.key, 10);

        db.saveRecord(session, reps.id, values);

        assertEquals(1, db.recordCount(sessionId));
        assertEquals(10, ((Number) db.previousValue(tracker.id, reps.id, reps.key)).intValue());
        db.deleteSession(sessionId);
        assertEquals(0, db.sessions().size());
        assertSame(TrackingDatabase.NO_PREVIOUS, db.previousValue(tracker.id, reps.id, reps.key));
    }

    @Test
    public void deleteTrackerRemovesSessionsToo() {
        Tracker tracker = db.trackers().get(0);
        long sessionId = db.createSession(tracker.id);

        db.deleteTracker(tracker.id);

        assertEquals(0, db.trackers().size());
        assertEquals(0, db.sessions().size());
        assertEquals(0, db.recordCount(sessionId));
    }

    @Test
    public void createSessionUsesNewId() {
        Tracker tracker = db.trackers().get(0);

        long first = db.createSession(tracker.id);
        long second = db.createSession(tracker.id);

        assertNotEquals(first, second);
        assertNotNull(db.session(second));
    }

    @Test
    public void overviewOrderPersistsForTrackersAndSessions() {
        Tracker seed = db.trackers().get(0);
        long secondTrackerId = db.insertTracker(db.getWritableDatabase(), "Second", "");
        long firstSessionId = db.createSession(seed.id);
        long secondSessionId = db.createSession(seed.id);

        assertEquals(secondTrackerId, db.trackers().get(0).id);
        assertEquals(secondSessionId, db.sessions().get(0).id);

        db.reorderTrackers(Arrays.asList(seed.id, secondTrackerId));
        db.reorderSessions(Arrays.asList(firstSessionId, secondSessionId));
        db.close();
        db = new TrackingDatabase(context);

        assertEquals(seed.id, db.trackers().get(0).id);
        assertEquals(secondTrackerId, db.trackers().get(1).id);
        assertEquals(firstSessionId, db.sessions().get(0).id);
        assertEquals(secondSessionId, db.sessions().get(1).id);
    }

    @Test
    public void upgradeFromVersionFiveAddsOverviewOrderAndKeepsOldSort() {
        db.close();
        context.deleteDatabase("tracking.sqlite");
        SQLiteDatabase old = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath("tracking.sqlite"), null);
        old.execSQL("CREATE TABLE trackers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,description TEXT,createdAt INTEGER NOT NULL,updatedAt INTEGER NOT NULL)");
        old.execSQL("CREATE TABLE fields(id INTEGER PRIMARY KEY AUTOINCREMENT,trackerId INTEGER NOT NULL,itemTitle TEXT,itemOrder INTEGER NOT NULL DEFAULT 0,fieldKey TEXT NOT NULL,label TEXT NOT NULL,type TEXT NOT NULL,sortOrder INTEGER NOT NULL,defaultValue TEXT,incrementValue REAL,unit TEXT,required INTEGER NOT NULL,prefillFromPrevious INTEGER NOT NULL)");
        old.execSQL("CREATE TABLE sessions(id INTEGER PRIMARY KEY AUTOINCREMENT,trackerId INTEGER NOT NULL,createdAt INTEGER NOT NULL,updatedAt INTEGER NOT NULL)");
        old.execSQL("CREATE TABLE field_records(id INTEGER PRIMARY KEY AUTOINCREMENT,sessionId INTEGER NOT NULL,trackerId INTEGER NOT NULL,fieldId INTEGER NOT NULL,fieldKey TEXT NOT NULL,valuesJson TEXT NOT NULL,createdAt INTEGER NOT NULL,updatedAt INTEGER NOT NULL,UNIQUE(sessionId,fieldId))");
        old.execSQL("INSERT INTO trackers(id,name,description,createdAt,updatedAt) VALUES(1,'Old','',100,100)");
        old.execSQL("INSERT INTO trackers(id,name,description,createdAt,updatedAt) VALUES(2,'New','',100,200)");
        old.execSQL("INSERT INTO sessions(id,trackerId,createdAt,updatedAt) VALUES(1,1,100,100)");
        old.execSQL("INSERT INTO sessions(id,trackerId,createdAt,updatedAt) VALUES(2,1,200,200)");
        old.setVersion(5);
        old.close();

        db = new TrackingDatabase(context);

        assertEquals(2, db.trackers().get(0).id);
        assertEquals(1, db.trackers().get(1).id);
        assertEquals(2, db.sessions().get(0).id);
        assertEquals(1, db.sessions().get(1).id);
    }
}
