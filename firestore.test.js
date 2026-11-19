const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const MAHMOUD_UID = "mahmoud_uid_001";
const NOUR_UID = "nour_uid_002";
const STRANGER_UID = "stranger_uid_999";
const SANCTUARY_ID = "sanctuary_mahmoud_uid_001";
const INVITE_CODE = "H7K9M2";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msAgo = 2000) {
  return new Date(Date.now() - msAgo);
}

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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = pastDate();

  await assertFails(unauthDb.collection("users").doc(MAHMOUD_UID).get());
  await assertFails(unauthDb.collection("sanctuaries").doc(SANCTUARY_ID).get());
  await assertFails(unauthDb.collection("prayers_daily").get());
  await assertFails(unauthDb.collection("dhikr_counts").get());
  await assertFails(unauthDb.collection("duas").get());
  await assertFails(unauthDb.collection("shared_goals").get());
  await assertFails(unauthDb.collection("notifications_queue").get());

  await assertFails(
    unauthDb.collection("duas").doc("dua_unauth").set({
      sanctuaryId: SANCTUARY_ID,
      userId: MAHMOUD_UID,
      memberUids: [MAHMOUD_UID],
      senderRole: "mahmoud",
      content: "اللهم ارزقنا الخير",
      tag: "rizq",
      ameenByMahmoud: false,
      ameenByNour: false,
      pinned: false,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("User profiles: strictly owner-only, rejects list, ghost fields, and future timestamps", async () => {
  const mahmoudDb = testEnv.authenticatedContext(MAHMOUD_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const now = pastDate(5000);

  await assertSucceeds(
    mahmoudDb.collection("users").doc(MAHMOUD_UID).set({
      userId: MAHMOUD_UID,
      displayName: "محمود",
      activeRole: "mahmoud",
      sanctuaryId: SANCTUARY_ID,
      darkModeEnabled: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update allowed
  await assertSucceeds(
    mahmoudDb.collection("users").doc(MAHMOUD_UID).update({
      darkModeEnabled: true,
      updatedAt: pastDate(1000),
    })
  );

  // Reject listing users collection (PII guard)
  await assertFails(mahmoudDb.collection("users").get());

  // Reject stranger reading or editing Mahmoud's profile
  await assertFails(strangerDb.collection("users").doc(MAHMOUD_UID).get());
  await assertFails(
    strangerDb.collection("users").doc(MAHMOUD_UID).update({
      displayName: "Hijack",
      updatedAt: pastDate(500),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    mahmoudDb.collection("users").doc(MAHMOUD_UID).update({
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Reject shadow field or @DocumentId 'id' in payload
  await assertFails(
    mahmoudDb.collection("users").doc(NOUR_UID).set({
      id: NOUR_UID,
      userId: NOUR_UID,
      displayName: "نور",
      activeRole: "nour",
      sanctuaryId: SANCTUARY_ID,
      darkModeEnabled: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject tampered future timestamp
  await assertFails(
    mahmoudDb.collection("users").doc(MAHMOUD_UID).update({
      darkModeEnabled: false,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Sanctuaries & Invite Codes: Mahmoud creates, Nour joins (max 2 members), Stranger blocked", async () => {
  const mahmoudDb = testEnv.authenticatedContext(MAHMOUD_UID).firestore();
  const nourDb = testEnv.authenticatedContext(NOUR_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const now = pastDate(5000);

  await assertSucceeds(
    mahmoudDb.collection("sanctuaries").doc(SANCTUARY_ID).set({
      ownerUid: MAHMOUD_UID,
      memberUids: [MAHMOUD_UID],
      inviteCode: INVITE_CODE,
      mahmoudPinHash: "a".repeat(64),
      nourPinHash: "",
      mahmoudStreak: 0,
      nourStreak: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    mahmoudDb.collection("invite_codes").doc(INVITE_CODE).set({
      sanctuaryId: SANCTUARY_ID,
      ownerUid: MAHMOUD_UID,
      createdAt: now,
    })
  );

  // Invite codes can be looked up by exact code, never listed
  await assertSucceeds(nourDb.collection("invite_codes").doc(INVITE_CODE).get());
  await assertFails(nourDb.collection("invite_codes").get());

  // Nour joins sanctuary (adds her UID as 2nd member)
  await assertSucceeds(
    nourDb.collection("sanctuaries").doc(SANCTUARY_ID).update({
      memberUids: [MAHMOUD_UID, NOUR_UID],
      updatedAt: pastDate(1000),
    })
  );

  // Stranger cannot join once 2 members are already present
  await assertFails(
    strangerDb.collection("sanctuaries").doc(SANCTUARY_ID).update({
      memberUids: [MAHMOUD_UID, NOUR_UID, STRANGER_UID],
      updatedAt: pastDate(500),
    })
  );

  // Both members can query sanctuaries with memberUids filter; stranger cannot read
  await assertSucceeds(
    nourDb.collection("sanctuaries").where("memberUids", "array-contains", NOUR_UID).get()
  );
  await assertFails(strangerDb.collection("sanctuaries").doc(SANCTUARY_ID).get());
  await assertFails(mahmoudDb.collection("sanctuaries").get());
});

test("Prayers Daily: Kotlin create/update & memberUids query succeed; stranger & unfiltered fail", async () => {
  const mahmoudDb = testEnv.authenticatedContext(MAHMOUD_UID).firestore();
  const nourDb = testEnv.authenticatedContext(NOUR_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const now = pastDate(5000);
  const docId = `${SANCTUARY_ID}_2026-11-19`;

  await assertSucceeds(
    mahmoudDb.collection("prayers_daily").doc(docId).set({
      sanctuaryId: SANCTUARY_ID,
      userId: MAHMOUD_UID,
      memberUids: [MAHMOUD_UID, NOUR_UID],
      dateKey: "2026-11-19",
      mahmoudFajr: true,
      mahmoudDhuhr: false,
      mahmoudAsr: false,
      mahmoudMaghrib: false,
      mahmoudIsha: false,
      nourFajr: false,
      nourDhuhr: false,
      nourAsr: false,
      nourMaghrib: false,
      nourIsha: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Nour checks off her Fajr prayer
  await assertSucceeds(
    nourDb.collection("prayers_daily").doc(docId).update({
      nourFajr: true,
      updatedAt: pastDate(1000),
    })
  );

  // Filtered query matches Kotlin repository
  await assertSucceeds(
    mahmoudDb.collection("prayers_daily").where("memberUids", "array-contains", MAHMOUD_UID).get()
  );

  // Unfiltered query and stranger access rejected
  await assertFails(mahmoudDb.collection("prayers_daily").get());
  await assertFails(strangerDb.collection("prayers_daily").doc(docId).get());
});

test("Dhikr Counts: Kotlin multi-period create/increment succeed; invalid dhikrKey & stranger fail", async () => {
  const mahmoudDb = testEnv.authenticatedContext(MAHMOUD_UID).firestore();
  const nourDb = testEnv.authenticatedContext(NOUR_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const now = pastDate(5000);
  const docId = `${SANCTUARY_ID}_mahmoud_istighfar`;

  await assertSucceeds(
    mahmoudDb.collection("dhikr_counts").doc(docId).set({
      sanctuaryId: SANCTUARY_ID,
      userId: MAHMOUD_UID,
      memberUids: [MAHMOUD_UID, NOUR_UID],
      role: "mahmoud",
      dhikrKey: "istighfar",
      dailyCount: 500,
      dailyDateKey: "2026-11-19",
      weeklyCount: 1200,
      weeklyKey: "2026-W47",
      lifetimeCount: 4500,
      lastMilestoneNotified: 500,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    mahmoudDb.collection("dhikr_counts").doc(docId).update({
      dailyCount: 501,
      weeklyCount: 1201,
      lifetimeCount: 4501,
      updatedAt: pastDate(1000),
    })
  );

  // Nour observes Mahmoud's live counts
  await assertSucceeds(
    nourDb.collection("dhikr_counts").where("memberUids", "array-contains", NOUR_UID).get()
  );

  // Reject invalid dhikrKey
  await assertFails(
    mahmoudDb.collection("dhikr_counts").doc(`${SANCTUARY_ID}_mahmoud_bad`).set({
      sanctuaryId: SANCTUARY_ID,
      userId: MAHMOUD_UID,
      memberUids: [MAHMOUD_UID],
      role: "mahmoud",
      dhikrKey: "invalid_dhikr",
      dailyCount: 1,
      dailyDateKey: "2026-11-19",
      weeklyCount: 1,
      weeklyKey: "2026-W47",
      lifetimeCount: 1,
      lastMilestoneNotified: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(strangerDb.collection("dhikr_counts").doc(docId).get());
});

test("Duas, Shared Goals & Notifications Queue: full partner workflow succeeds; strangers & invalid mutations fail", async () => {
  const mahmoudDb = testEnv.authenticatedContext(MAHMOUD_UID).firestore();
  const nourDb = testEnv.authenticatedContext(NOUR_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const now = pastDate(5000);

  // 1. Shared Dua create + partner Ameen & pin update
  await assertSucceeds(
    mahmoudDb.collection("duas").doc("dua_001").set({
      sanctuaryId: SANCTUARY_ID,
      userId: MAHMOUD_UID,
      memberUids: [MAHMOUD_UID, NOUR_UID],
      senderRole: "mahmoud",
      content: "اللهم ارزقنا رزقاً حلالاً طيباً واجمعنا في الحلال على خير",
      tag: "marriage",
      ameenByMahmoud: true,
      ameenByNour: false,
      pinned: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    nourDb.collection("duas").doc("dua_001").update({
      ameenByNour: true,
      pinned: true,
      updatedAt: pastDate(1000),
    })
  );

  // Content is immutable on Dua update
  await assertFails(
    nourDb.collection("duas").doc("dua_001").update({
      content: "Tampered text",
      updatedAt: pastDate(500),
    })
  );

  await assertSucceeds(
    nourDb.collection("duas").where("memberUids", "array-contains", NOUR_UID).get()
  );

  // 2. Shared Goal create + progress & adhkar update
  const goalId = `${SANCTUARY_ID}_main`;
  await assertSucceeds(
    mahmoudDb.collection("shared_goals").doc(goalId).set({
      sanctuaryId: SANCTUARY_ID,
      userId: MAHMOUD_UID,
      memberUids: [MAHMOUD_UID, NOUR_UID],
      title: "10,000 استغفار للرزق والزواج في الحلال",
      dhikrKey: "istighfar",
      period: "monthly",
      targetCount: 10000,
      mahmoudProgress: 500,
      nourProgress: 320,
      adhkarDateKey: "2026-11-19",
      mahmoudMorningAdhkar: ["ayat_kursi"],
      mahmoudEveningAdhkar: [],
      nourMorningAdhkar: [],
      nourEveningAdhkar: [],
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    nourDb.collection("shared_goals").doc(goalId).update({
      nourProgress: 400,
      nourMorningAdhkar: ["ayat_kursi", "ikhlas_muawwidhat"],
      updatedAt: pastDate(1000),
    })
  );

  await assertSucceeds(
    nourDb.collection("shared_goals").where("memberUids", "array-contains", NOUR_UID).get()
  );

  // 3. Notifications Queue create + mark delivered
  await assertSucceeds(
    mahmoudDb.collection("notifications_queue").doc("notif_001").set({
      sanctuaryId: SANCTUARY_ID,
      userId: MAHMOUD_UID,
      memberUids: [MAHMOUD_UID, NOUR_UID],
      senderRole: "mahmoud",
      recipientRole: "nour",
      type: "prayer",
      title: "تقبل الله صلاتك 🕌",
      body: "محمود صلى صلاة الفجر 🕌 تقبل الله، الدور عليكِ يا نور 🤍",
      delivered: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    nourDb.collection("notifications_queue").doc("notif_001").update({
      delivered: true,
      updatedAt: pastDate(1000),
    })
  );

  await assertSucceeds(
    nourDb
      .collection("notifications_queue")
      .where("memberUids", "array-contains", NOUR_UID)
      .get()
  );

  // Stranger rejected across all three
  await assertFails(strangerDb.collection("duas").doc("dua_001").get());
  await assertFails(strangerDb.collection("shared_goals").doc(goalId).get());
  await assertFails(strangerDb.collection("notifications_queue").doc("notif_001").get());
});
