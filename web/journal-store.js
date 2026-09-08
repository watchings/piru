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
