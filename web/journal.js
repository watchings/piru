import { addDose, listDoses } from "./journal-store.js";

const form = document.querySelector("#dose-form");
const doses = document.querySelector("#doses");

function renderDoses(entries) {
  doses.replaceChildren(...entries.map(entry => {
    const row = document.createElement("li");
    row.textContent = `${entry.substance} — ${entry.amount} ${entry.unit} (${entry.route})`;
    return row;
  }));
}

export async function refreshDoses() {
  renderDoses(await listDoses());
}

form.addEventListener("submit", async event => {
  event.preventDefault();
  const data = new FormData(form);
  await addDose(Object.fromEntries(data.entries()));
  form.reset();
  await refreshDoses();
});

refreshDoses().catch(() => {
  doses.textContent = "Local journal storage is unavailable in this browser.";
});

window.addEventListener("piru-journal-changed", () => {
  refreshDoses().catch(() => {
    doses.textContent = "Local journal storage is unavailable in this browser.";
  });
});
