const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read sensors", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("sensors").get());
});

test("Unauthenticated user: cannot write sensor", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("sensors").doc("sensor_1").set({
      id: "sensor_1",
      name: "MAP Sensor 3 Bar",
      category: "MAP_SENSOR",
      authorNick: "Anon",
      authorId: ALICE_UID,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can read sensors", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("sensors").doc("sensor_1").set({
      id: "sensor_1",
      name: "MAP Sensor 3 Bar",
      category: "MAP_SENSOR",
      authorNick: "Alice",
      authorId: ALICE_UID,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("sensors").get());
  await assertSucceeds(bobDb.collection("sensors").doc("sensor_1").get());
});

test("Authenticated user: can create a valid sensor with own authorId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("sensors").doc("sensor_alice_1").set({
      id: "sensor_alice_1",
      name: "Bosch 0281002401 3 Bar",
      partNumber: "0281002401",
      category: "MAP_SENSOR",
      shortDescription: "3 bar MAP sensor",
      imageUrl: "",
      vectorIllustrationKey: "map_3b",
      authorNick: "AliceTuner",
      authorId: ALICE_UID,
      createdAt: new Date(),
      updatedAt: new Date(),
      rating: 5.0,
      ratingVotesCount: 1,
      pullupRequired: false,
      pullupRecommendedValue: "",
      pullupNote: "",
      importantNotes: "",
      pins: [],
      characteristic: { type: "VOLTAGE_PRESSURE", title: "Linear", inputLabel: "V", outputLabel: "kPa", points: [], formulaOrNotes: "" },
      isUserAdded: true,
    })
  );
});

test("Authenticated user: cannot create sensor with someone else's authorId (identity spoofing)", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("sensors").doc("sensor_spoof").set({
      id: "sensor_spoof",
      name: "Spoofed Sensor",
      category: "MAP_SENSOR",
      authorNick: "Bob",
      authorId: ALICE_UID, // Spoofing Alice!
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: author can delete own sensor", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("sensors").doc("sensor_alice_del").set({
      id: "sensor_alice_del",
      name: "ToDelete",
      category: "OTHER",
      authorNick: "Alice",
      authorId: ALICE_UID,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("sensors").doc("sensor_alice_del").delete());
});
