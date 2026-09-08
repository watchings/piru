import { loadCatalog, searchCatalog } from "./catalog.js";
import { listDoses, replaceDoses } from "./journal-store.js";

const status = document.querySelector("#status");
const results = document.querySelector("#results");

async function exportEncryptedBackup() {
  const passphrase = window.prompt("Choose a passphrase for this encrypted backup.");
  if (!passphrase) {
    status.textContent = "Backup cancelled. Your local data was not changed.";
    return;
  }

  async function importEncryptedBackup(file) {
    const passphrase = window.prompt("Enter the backup passphrase.");
    if (!passphrase) return;
    const envelope = JSON.parse(await file.text());
    if (envelope.version !== 1 || envelope.algorithm !== "PBKDF2-SHA-256/AES-256-GCM") {
      throw new Error("Unsupported backup");
    }
    const material = await crypto.subtle.importKey(
      "raw", new TextEncoder().encode(passphrase), "PBKDF2", false, ["deriveKey"]);
    const key = await crypto.subtle.deriveKey(
      { name: "PBKDF2", salt: new Uint8Array(envelope.salt), iterations: 250000, hash: "SHA-256" },
      material, { name: "AES-GCM", length: 256 }, false, ["decrypt"]);
    const plaintext = await crypto.subtle.decrypt(
      { name: "AES-GCM", iv: new Uint8Array(envelope.iv) },
      key, new Uint8Array(envelope.ciphertext));
    const document = JSON.parse(new TextDecoder().decode(plaintext));
    if (document.piruExportVersion !== 1 || !Array.isArray(document.orphanDoses)) {
      throw new Error("Invalid Piru Native export");
    }
    await replaceDoses(document.orphanDoses);
    status.textContent = "Backup imported. The local journal was replaced.";
    window.dispatchEvent(new Event("piru-journal-changed"));
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

document.querySelector("#import").addEventListener("change", event => {
  const [file] = event.target.files;
  if (!file) return;
  importEncryptedBackup(file).catch(() => {
    status.textContent = "Backup import failed. Existing local data was not changed.";
  });

  document.querySelector("#notify").addEventListener("click", async () => {
    if (!("Notification" in window)) {
      status.textContent = "Notifications are unavailable in this browser.";
      return;
    }
    const permission = await Notification.requestPermission();
    status.textContent = permission === "granted"
      ? "Reminders enabled for this browser."
      : "Notifications remain disabled.";
  });

  document.querySelector("#print").addEventListener("click", () => window.print());

  document.querySelector("#scan-barcode").addEventListener("click", async () => {
    if (!("BarcodeDetector" in window)) {
      status.textContent = "Barcode scanning is unavailable; enter the barcode manually.";
      return;
    }
    status.textContent = "Barcode camera scanning requires a camera-enabled browser.";
  });
  event.target.value = "";
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
