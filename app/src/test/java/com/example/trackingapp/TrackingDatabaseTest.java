package com.example.trackingapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
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
}
