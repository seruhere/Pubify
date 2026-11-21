const test = require("node:test");
const fs = require("node:fs");
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");

const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-pdftoepu-b5225247-0949-492f-8bee-cec45a0fe376";

const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

let testEnv;

function pastTimestamp() {
  return new Date(Date.now() - 1000);
}

function futureTimestamp() {
  return new Date(Date.now() + 60_000);
}

function validUserProfile(userId = ALICE_UID, overrides = {}) {
  return {
    userId,
    displayName: "Alice Reader",
    email: "alice@example.com",
    photoUrl: "https://example.com/alice.png",
    totalBooksSynced: 3,
    createdAt: pastTimestamp(),
    updatedAt: pastTimestamp(),
    ...overrides,
  };
}

function validBookPayload(userId = ALICE_UID, overrides = {}) {
  return {
    userId,
    title: "The Art of Clean Architecture",
    author: "Robert Martin",
    originalFileName: "clean_architecture.pdf",
    fileSizeBytes: 2450120,
    pageCount: 320,
    chapterCount: 14,
    lastReadChapterIndex: 4,
    favorite: true,
    completed: false,
    convertedAtMillis: 1700000000000,
    createdAt: pastTimestamp(),
    updatedAt: pastTimestamp(),
    ...overrides,
  };
}

function validSessionPayload(userId = ALICE_UID, overrides = {}) {
  return {
    userId,
    bookKey: "book_1_1700000000000",
    bookTitle: "The Art of Clean Architecture",
    durationSeconds: 1800,
    pagesRead: 24,
    startPage: 10,
    endPage: 34,
    timestampMillis: 1700001000000,
    createdAt: pastTimestamp(),
    ...overrides,
  };
}

function validHighlightPayload(userId = ALICE_UID, overrides = {}) {
  return {
    userId,
    bookKey: "book_1_1700000000000",
    bookTitle: "The Art of Clean Architecture",
    chapterIndex: 2,
    chapterTitle: "Chapter 2",
    paragraphIndex: 5,
    text: "Good architecture makes the system easy to understand and change.",
    colorHex: 0xffffeb3b,
    note: "Key design principle",
    scope: "PASSAGE",
    createdAt: pastTimestamp(),
    ...overrides,
  };
}

function validBookmarkPayload(userId = ALICE_UID, overrides = {}) {
  return {
    userId,
    bookKey: "book_1_1700000000000",
    bookTitle: "The Art of Clean Architecture",
    chapterIndex: 3,
    chapterTitle: "Chapter 3",
    pageNumber: 42,
    totalBookPages: 320,
    scrollOffsetFraction: 0.45,
    snippet: "Boundaries separate high-level policy from low-level details.",
    createdAt: pastTimestamp(),
    ...overrides,
  };
}

test.before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      host: "127.0.0.1",
      port: 8085,
      rules: fs.readFileSync("firestore.rules", "utf8"),
    },
  });
});

test.after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

test.beforeEach(async () => {
  await testEnv.clearFirestore();
});

function dbFor(uid) {
  const ctx = uid ? testEnv.authenticatedContext(uid) : testEnv.unauthenticatedContext();
  return ctx.firestore({ databaseId: DATABASE_ID });
}

test("Unauthenticated users are denied all read/write access", async () => {
  const anonDb = dbFor(null);
  await assertFails(anonDb.collection("users").doc(ALICE_UID).get());
  await assertFails(anonDb.collection("users").doc(ALICE_UID).set(validUserProfile()));
  await assertFails(anonDb.collection("users").doc(ALICE_UID).collection("books").get());
  await assertFails(anonDb.collection("unknown_root").doc("doc1").get());
});

test("UserProfile: owner can create, read, update mutable fields, and delete; cannot list /users or spoof owner", async () => {
  const aliceDb = dbFor(ALICE_UID);
  const bobDb = dbFor(BOB_UID);

  // Create valid profile (matches CloudUserProfile.toCreateMap)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID))
  );

  // Owner can get own profile
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Listing /users collection is strictly forbidden even for authenticated users
  await assertFails(aliceDb.collection("users").get());
  await assertFails(bobDb.collection("users").get());

  // Cross-user read/write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hacked" }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).delete());

  // Valid update matching CloudUserProfile.toUpdateMap
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Updated",
      totalBooksSynced: 12,
      updatedAt: pastTimestamp(),
    })
  );

  // Reject mutating immutable createdAt or userId
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      userId: BOB_UID,
      updatedAt: pastTimestamp(),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Future Alice",
      updatedAt: futureTimestamp(),
    })
  );

  // Reject ghost field / id field in payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      id: ALICE_UID,
      updatedAt: pastTimestamp(),
    })
  );

  // Owner can delete own profile
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).delete());
});

test("CloudBookRecord: owner CRUD, list, partial update, immutability, and schema validation", async () => {
  const aliceDb = dbFor(ALICE_UID);
  const bobDb = dbFor(BOB_UID);
  const bookRef = aliceDb.collection("users").doc(ALICE_UID).collection("books").doc("book_1_1700000000000");

  // Create valid book (matches CloudBookRecord.toCreateMap)
  await assertSucceeds(bookRef.set(validBookPayload(ALICE_UID)));

  // Owner can get and list their own books
  await assertSucceeds(bookRef.get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("books").get());

  // Cross-user cannot read, list, create, or delete Alice's books
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("books").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("books").doc("book_1_1700000000000").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("books").doc("book_evil").set(validBookPayload(ALICE_UID))
  );

  // Valid update matching CloudBookRecord.toUpdateMap
  await assertSucceeds(
    bookRef.update({
      lastReadChapterIndex: 8,
      completed: true,
      favorite: false,
      updatedAt: pastTimestamp(),
    })
  );

  // Reject immutable field mutation (createdAt / userId)
  await assertFails(bookRef.update({ createdAt: pastTimestamp(), updatedAt: pastTimestamp() }));
  await assertFails(bookRef.update({ userId: BOB_UID, updatedAt: pastTimestamp() }));

  // Reject extra `id` field or ghost field on create
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("books")
      .doc("book_2_1700000000001")
      .set(validBookPayload(ALICE_UID, { id: "book_2_1700000000001" }))
  );

  // Reject invalid future timestamp or negative pageCount
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("books")
      .doc("book_future")
      .set(validBookPayload(ALICE_UID, { createdAt: futureTimestamp() }))
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("books")
      .doc("book_neg")
      .set(validBookPayload(ALICE_UID, { pageCount: -5 }))
  );

  // Owner can delete book
  await assertSucceeds(bookRef.delete());
});

test("CloudReadingSession: immutable create-only sessions with owner list & delete", async () => {
  const aliceDb = dbFor(ALICE_UID);
  const bobDb = dbFor(BOB_UID);
  const sessRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("reading_sessions")
    .doc("sess_1_1700001000000");

  await assertSucceeds(sessRef.set(validSessionPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("reading_sessions").get());

  // Sessions are immutable (allow update: if false)
  await assertFails(sessRef.update({ pagesRead: 99 }));

  // Cross-user denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("reading_sessions").get());

  // Reject excessive duration (> 86400s)
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("reading_sessions")
      .doc("sess_invalid")
      .set(validSessionPayload(ALICE_UID, { durationSeconds: 999999 }))
  );

  await assertSucceeds(sessRef.delete());
});

test("CloudHighlight & CloudBookmark: strict enum/range validation, partial update, and owner isolation", async () => {
  const aliceDb = dbFor(ALICE_UID);
  const bobDb = dbFor(BOB_UID);

  // Highlight create, list, note/color update, and invalid scope rejection
  const hlRef = aliceDb.collection("users").doc(ALICE_UID).collection("highlights").doc("hl_1_1700002000000");
  await assertSucceeds(hlRef.set(validHighlightPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("highlights").get());
  await assertSucceeds(hlRef.update({ note: "Updated note", colorHex: 0xff81c784 }));
  await assertFails(hlRef.update({ text: "Cannot mutate highlighted text" }));
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("highlights")
      .doc("hl_bad_scope")
      .set(validHighlightPayload(ALICE_UID, { scope: "INVALID_SCOPE" }))
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("highlights").get());
  await assertSucceeds(hlRef.delete());

  // Bookmark create, list, immutable update rejection, scrollOffsetFraction range check
  const bmRef = aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("bm_1_1700003000000");
  await assertSucceeds(bmRef.set(validBookmarkPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").get());
  await assertFails(bmRef.update({ pageNumber: 55 }));
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bookmarks")
      .doc("bm_bad_scroll")
      .set(validBookmarkPayload(ALICE_UID, { scrollOffsetFraction: 1.5 }))
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("bookmarks").get());
  await assertSucceeds(bmRef.delete());
});
