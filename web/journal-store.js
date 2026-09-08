const DATABASE = "piru-journal";
const STORE = "dose-entries";
let databasePromise;

function database() {
  if (!databasePromise) {
    databasePromise = new Promise((resolve, reject) => {
      const request = indexedDB.open(DATABASE, 1);
      request.onupgradeneeded = () => request.result.createObjectStore(STORE, { keyPath: "id" });
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
  }
  return databasePromise;
}

export async function listDoses() {
  const db = await database();
  return new Promise((resolve, reject) => {
    const request = db.transaction(STORE, "readonly").objectStore(STORE).getAll();
    request.onsuccess = () => resolve(request.result.sort((a, b) => b.timestamp - a.timestamp));
    request.onerror = () => reject(request.error);
  });
}

export async function addDose({ substance, amount, unit, route, notes = "" }) {
  const dose = {
    id: crypto.randomUUID(),
    substance,
    amount: Number(amount),
    unit,
    route,
    timestamp: Date.now(),
    notes,
    tags: [],
    isBackgroundMed: false
  };
  const db = await database();
  return new Promise((resolve, reject) => {
    const request = db.transaction(STORE, "readwrite").objectStore(STORE).add(dose);
    request.onsuccess = () => resolve(dose);
    request.onerror = () => reject(request.error);
  });
}

export async function updateDose(id, changes) {
  const db = await database();
  return new Promise((resolve, reject) => {
    const transaction = db.transaction(STORE, "readwrite");
    const store = transaction.objectStore(STORE);
    const request = store.get(id);
    request.onsuccess = () => {
      if (!request.result) {
        reject(new Error("Dose not found"));
        return;
      }
      store.put({ ...request.result, ...changes, id });
    };
    transaction.oncomplete = resolve;
    transaction.onerror = () => reject(transaction.error);
  });
}

export async function replaceDoses(entries) {
  if (!Array.isArray(entries) || entries.some(entry =>
    !entry.id || !entry.substance || !Number.isFinite(entry.amount) ||
    entry.amount < 0 || !entry.unit || !entry.route || !Number.isFinite(entry.timestamp)
  )) {
    throw new Error("Invalid journal export");
  }
  const db = await database();
  return new Promise((resolve, reject) => {
    const transaction = db.transaction(STORE, "readwrite");
    const store = transaction.objectStore(STORE);
    store.clear();
    entries.forEach(entry => store.add(entry));
    transaction.oncomplete = resolve;
    transaction.onerror = () => reject(transaction.error);
  });
}

export async function deleteDose(id) {
  const db = await database();
  return new Promise((resolve, reject) => {
    const request = db.transaction(STORE, "readwrite").objectStore(STORE).delete(id);
    request.onsuccess = resolve;
    request.onerror = () => reject(request.error);
  });
}
