import { loadCatalog, searchCatalog } from "./catalog.js";
import { listDoses } from "./journal-store.js";

const status = document.querySelector("#status");
const results = document.querySelector("#results");

async function exportEncryptedBackup() {
  const passphrase = window.prompt("Choose a passphrase for this encrypted backup.");
  if (!passphrase) {
    status.textContent = "Backup cancelled. Your local data was not changed.";
    return;
  }
  const doses = await listDoses();
  const payload = new TextEncoder().encode(JSON.stringify({
    piruExportVersion: 1,
    appVersion: "Piru Web 0.1.0",
    exportedAt: Date.now(),
    sessions: [],
    orphanDoses: doses,
    dailyDoseItems: [],
    substanceColors: [],
    userColors: [],
    favorites: [],
    customSubstances: []
  }));
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const material = await crypto.subtle.importKey(
    "raw", new TextEncoder().encode(passphrase), "PBKDF2", false, ["deriveKey"]);
  const key = await crypto.subtle.deriveKey(
    { name: "PBKDF2", salt, iterations: 250000, hash: "SHA-256" },
    material, { name: "AES-GCM", length: 256 }, false, ["encrypt"]);
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const ciphertext = await crypto.subtle.encrypt({ name: "AES-GCM", iv }, key, payload);
  const envelope = { version: 1, algorithm: "PBKDF2-SHA-256/AES-256-GCM",
    salt: [...salt], iv: [...iv], ciphertext: [...new Uint8Array(ciphertext)] };
  const blob = new Blob([JSON.stringify(envelope)], { type: "application/json" });
  const link = document.createElement("a");
  link.href = URL.createObjectURL(blob);
  link.download = "piru-encrypted-backup.json";
  link.click();
  URL.revokeObjectURL(link.href);
  status.textContent = "Backup exported. Store it somewhere private.";
}

document.querySelector("#export").addEventListener("click", () => {
  exportEncryptedBackup().catch(() => {
    status.textContent = "Backup export failed. Your local data was not changed.";
  });
});

function renderResults(items) {
  results.replaceChildren(...items.map(item => {
    const row = document.createElement("li");
    row.textContent = `${item.display_name || item.name} — ${item.category || "Uncategorized"}`;
    return row;
  }));
}

document.querySelector("#search").addEventListener("input", event => {
  renderResults(searchCatalog(event.target.value));
});

loadCatalog()
  .then(items => {
    status.textContent = `${items.length} substances available offline after first load.`;
    renderResults(items.slice(0, 20));
  })
  .catch(() => {
    status.textContent = "The substance library is unavailable. Check the catalog update.";
  });

if ("serviceWorker" in navigator) {
  navigator.serviceWorker.register("sw.js");
}
