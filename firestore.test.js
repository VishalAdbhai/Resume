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

function validCreatePayload(uid, now = new Date(Date.now() - 5000)) {
  return {
    userId: uid,
    title: "Staff Android Engineer CV",
    templateId: "CLASSIC",
    accentId: "INK_NAVY",
    pageSize: "A4",
    showPhotoInPdf: false,
    fullName: "Alice Rivera",
    headline: "Staff Android Engineer",
    email: "alice@example.com",
    phone: "+1 555-0192",
    location: "Seattle, WA",
    website: "https://alicerivera.dev",
    linkedIn: "linkedin.com/in/alicerivera",
    summary: "Android architect focused on Jetpack Compose and offline-first systems.",
    experiences: [
      {
        entryId: "exp_1",
        role: "Senior Android Engineer",
        company: "Northstar Labs",
        location: "Seattle, WA",
        startDate: "2022",
        endDate: "Present",
        currentlyWorking: true,
        highlights: "Led Compose migration across 14 screens.\nReduced cold start time by 32%."
      }
    ],
    educations: [
      {
        entryId: "edu_1",
        degree: "B.S. Computer Science",
        institution: "University of Washington",
        location: "Seattle, WA",
        dateRange: "2014 - 2018",
        details: "Magna Cum Laude"
      }
    ],
    skills: ["Kotlin", "Jetpack Compose", "Coroutines", "PDF Rendering"],
    projects: [
      {
        entryId: "proj_1",
        name: "ComposePDF",
        roleOrIssuer: "Open Source Author",
        yearOrLink: "2024",
        description: "Lightweight vector PDF layout library for Android."
      }
    ],
    createdAt: now,
    updatedAt: now,
  };
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

test("Unauthenticated user: cannot list, get, create, or delete resumes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("resumes").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("resumes").doc("res_1").set(validCreatePayload(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("resumes").doc("res_1").get()
  );
});

test("Authenticated owner: create, list, get, partial update, and delete succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const colRef = aliceDb.collection("users").doc(ALICE_UID).collection("resumes");

  // Empty list query succeeds without undefined existing() property errors
  await assertSucceeds(colRef.get());

  // Create resume matching Kotlin toCreateMap()
  await assertSucceeds(colRef.doc("res_1").set(validCreatePayload(ALICE_UID)));

  // Read single & list
  await assertSucceeds(colRef.doc("res_1").get());
  await assertSucceeds(colRef.get());

  // Partial update matching Kotlin toUpdateMap() (does NOT send userId or createdAt)
  await assertSucceeds(
    colRef.doc("res_1").update({
      title: "Principal Android Engineer CV",
      templateId: "MODERN",
      accentId: "TERRACOTTA",
      pageSize: "LETTER",
      showPhotoInPdf: true,
      fullName: "Alice Rivera",
      headline: "Principal Android Engineer",
      email: "alice@example.com",
      phone: "+1 555-0192",
      location: "Seattle, WA",
      website: "https://alicerivera.dev",
      linkedIn: "linkedin.com/in/alicerivera",
      summary: "Updated summary.",
      experiences: [],
      educations: [],
      skills: ["Kotlin", "Architecture"],
      projects: [],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Delete succeeds
  await assertSucceeds(colRef.doc("res_1").delete());
});

test("Cross-user isolation: Bob cannot read, create, update, or delete Alice's resumes", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("resumes")
      .doc("alice_res")
      .set(validCreatePayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceResRef = bobDb.collection("users").doc(ALICE_UID).collection("resumes").doc("alice_res");

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("resumes").get());
  await assertFails(aliceResRef.get());
  await assertFails(aliceResRef.update({ title: "Hijacked", updatedAt: new Date(Date.now() - 1000) }));
  await assertFails(aliceResRef.delete());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("resumes").doc("bob_res").set(validCreatePayload(BOB_UID))
  );
});

test("Red team schema & immutability guards: reject ghost fields, @DocumentId id field, invalid enums, future timestamps, and immutable mutations", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const colRef = aliceDb.collection("users").doc(ALICE_UID).collection("resumes");

  // 1. Reject `@DocumentId` 'id' field in create payload
  const withId = validCreatePayload(ALICE_UID);
  withId.id = "res_bad_id";
  await assertFails(colRef.doc("res_bad_id").set(withId));

  // 2. Reject ghost/extra field
  const withGhost = validCreatePayload(ALICE_UID);
  withGhost.isAdmin = true;
  await assertFails(colRef.doc("res_ghost").set(withGhost));

  // 3. Reject invalid template enum
  const badTemplate = validCreatePayload(ALICE_UID);
  badTemplate.templateId = "FANCY_UNKNOWN";
  await assertFails(colRef.doc("res_bad_tpl").set(badTemplate));

  // 4. Reject future timestamp
  const futurePayload = validCreatePayload(ALICE_UID, new Date(Date.now() + 60000));
  await assertFails(colRef.doc("res_future").set(futurePayload));

  // 5. Reject oversized skill list (> 50)
  const tooManySkills = validCreatePayload(ALICE_UID);
  tooManySkills.skills = Array.from({ length: 51 }, (_, i) => `Skill ${i}`);
  await assertFails(colRef.doc("res_skills").set(tooManySkills));

  // 6. Reject mutating createdAt or userId on update
  await assertSucceeds(colRef.doc("res_ok").set(validCreatePayload(ALICE_UID)));
  await assertFails(
    colRef.doc("res_ok").update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    colRef.doc("res_ok").update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
