/*
 * Copyright 2018 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.replayTests.examples;

import org.junit.Test;
import org.terasology.ReplayTestingEnvironment;
import org.terasology.TestUtils;
import org.terasology.engine.GameThread;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.internal.EventSystem;
import org.terasology.logic.location.LocationComponent;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.recording.EventSystemReplayImpl;
import org.terasology.recording.RecordAndReplayStatus;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.WorldProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * This class tests the same things as ExampleReplayTest, but instead of putting the game to run in a different thread
 * while the tests run in the main thread, this class uses the inverted approach, running the tests on a different
 * thread while the game runs on the main thread.
 */
public class InvertedExampleReplayTest {

    private ReplayTestingEnvironment environment = new ReplayTestingEnvironment();

    private Thread testThread1 = new Thread() {

        @Override
        public void run() {
            TestUtils.waitUntil(() -> (environment.isInitialised() && environment.getRecordAndReplayStatus() == RecordAndReplayStatus.REPLAYING));
            LocalPlayer localPlayer = CoreRegistry.get(LocalPlayer.class);
            TestUtils.waitUntil(() -> localPlayer.isValid());
            EntityRef character = localPlayer.getCharacterEntity();
            Vector3f initialPosition = new Vector3f(19.79358f, 13.511584f, 2.3982882f);
            LocationComponent location = character.getComponent(LocationComponent.class);
            assertEquals(initialPosition, location.getLocalPosition()); // check initial position.

            EventSystemReplayImpl eventSystem = (EventSystemReplayImpl) CoreRegistry.get(EventSystem.class);
            TestUtils.waitUntil(() -> eventSystem.getLastRecordedEventIndex() >= 1810); // tests in the middle of a replay needs "checkpoints" like this.
            location = character.getComponent(LocationComponent.class);
            assertNotEquals(initialPosition, location.getLocalPosition()); // checks that the player is not on the initial position after they moved.
            TestUtils.waitUntil(() -> environment.getRecordAndReplayStatus() == RecordAndReplayStatus.REPLAY_FINISHED);

            location = character.getComponent(LocationComponent.class);
            Vector3f finalPosition = new Vector3f(25.189344f, 13.406443f, 8.6651945f);
            assertEquals(finalPosition, location.getLocalPosition()); // checks final position
            environment.getHost().shutdown();
            GameThread.reset();
        }
    };

    private Thread testThread2 = new Thread() {

        @Override
        public void run() {
            TestUtils.waitUntil(() -> (environment.isInitialised() && environment.getRecordAndReplayStatus() == RecordAndReplayStatus.REPLAYING));
            Vector3i blockLocation1 = new Vector3i(26, 12, -3);
            Vector3i blockLocation2 = new Vector3i(26, 13, -3);
            Vector3i blockLocation3 = new Vector3i(26, 12, -2);

            //checks the block initial type of three chunks that will be modified during the replay.
            WorldProvider worldProvider = CoreRegistry.get(WorldProvider.class);
            TestUtils.waitUntil(() -> (!(worldProvider.getBlock(blockLocation1).getDisplayName().equals("Unloaded"))));
            assertEquals(worldProvider.getBlock(blockLocation1).getDisplayName(), "Grass");
            assertEquals(worldProvider.getBlock(blockLocation2).getDisplayName(), "Air");
            assertEquals(worldProvider.getBlock(blockLocation3).getDisplayName(), "Grass");

            TestUtils.waitUntil(() -> environment.getRecordAndReplayStatus() == RecordAndReplayStatus.REPLAY_FINISHED);

            //checks the same blocks again after the replay.
            assertEquals(worldProvider.getBlock(blockLocation1).getDisplayName(), "Grass");
            assertEquals(worldProvider.getBlock(blockLocation2).getDisplayName(), "Grass");
            assertEquals(worldProvider.getBlock(blockLocation3).getDisplayName(), "Air");
            environment.getHost().shutdown();
            GameThread.reset();
        }
    };

    @Test
    public void test1() throws Exception {
        testThread1.start();
        startReplay();
    }

    @Test
    public void test2() throws Exception {
        testThread2.start();
        startReplay();
    }

    private void startReplay() throws Exception {
        String replayTitle = "Example";
        environment.openReplay(replayTitle, true);
    }
}
