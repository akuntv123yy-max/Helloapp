const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: "127.0.0.1",
      port: 8085,
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

test("Unauthenticated user: cannot read users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
});

test("Authenticated user: can create own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@test.com",
      publicKey: "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: cannot impersonate other user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob Fake",
      email: "bob@test.com",
      publicKey: "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0",
      createdAt: new Date(),
    })
  );
});

test("Moments: signed-in user can create Moment and comment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const momentDoc = aliceDb.collection("moments").doc("m1");
  await assertSucceeds(
    momentDoc.set({
      momentId: "m1",
      authorId: ALICE_UID,
      authorName: "Alice",
      authorEmail: "alice@test.com",
      text: "Hari ini hari yang cerah!",
      backgroundStyle: "gradient_sunset",
      likesCount: 0,
      commentsCount: 0,
      createdAt: new Date(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  // Bob can comment on Alice's moment
  await assertSucceeds(
    bobDb.collection("moments").doc("m1").collection("comments").doc("c1").set({
      commentId: "c1",
      momentId: "m1",
      authorId: BOB_UID,
      authorName: "Bob",
      authorEmail: "bob@test.com",
      commentText: "Keren banget ceritanya!",
      createdAt: new Date(),
    })
  );
});

test("Encrypted messages: only participants can read conversation and messages", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("conversations").doc("conv_ab").set({
      conversationId: "conv_ab",
      participantUids: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      participantEmails: { [ALICE_UID]: "alice@test.com", [BOB_UID]: "bob@test.com" },
      createdAt: new Date(),
    })
  );

  // Alice sends encrypted message
  await assertSucceeds(
    aliceDb.collection("conversations").doc("conv_ab").collection("messages").doc("msg1").set({
      senderId: ALICE_UID,
      recipientId: BOB_UID,
      participantUids: [ALICE_UID, BOB_UID],
      ciphertext: "U2FsdGVkX1+vupppZksvRf5pq5g5XjFRIipRkw++0C1=...",
      iv: "dGVzdGl2MTIzNA==",
      encryptedKeySender: "encryptedKeyA",
      encryptedKeyRecipient: "encryptedKeyB",
      isEncrypted: true,
      createdAt: new Date(),
    })
  );

  // Charlie tries to read Alice & Bob conversation -> fails
  const charlieDb = testEnv.authenticatedContext("charlie_789").firestore();
  await assertFails(
    charlieDb.collection("conversations").doc("conv_ab").get()
  );
  await assertFails(
    charlieDb.collection("conversations").doc("conv_ab").collection("messages").doc("msg1").get()
  );
});
