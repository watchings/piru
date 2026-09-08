import { addDose, deleteDose, listDoses, updateDose } from "./journal-store.js";
import { remainingFraction } from "./pk.js";

const form = document.querySelector("#dose-form");
const doses = document.querySelector("#doses");

function renderDoses(entries) {
  doses.replaceChildren(...entries.map(entry => {
    const row = document.createElement("li");
    const elapsed = Math.max(0, (Date.now() - entry.timestamp) / 60000);
    const remaining = Math.round(remainingFraction(elapsed, 300) * 100);
    row.textContent = `${entry.substance} — ${entry.amount} ${entry.unit} (${entry.route}), ` +
      `estimated ${remaining}% remaining`;
    const remove = document.createElement("button");
    remove.type = "button";
    remove.textContent = "Delete";
    remove.addEventListener("click", async () => {
      await deleteDose(entry.id);
      await refreshDoses();
    });
    const edit = document.createElement("button");
    edit.type = "button";
    edit.textContent = "Edit";
    edit.addEventListener("click", async () => {
      const amount = window.prompt("Amount", String(entry.amount));
      if (amount === null) return;
      const parsed = Number(amount);
      if (!Number.isFinite(parsed) || parsed < 0) return;
      const notes = window.prompt("Note (optional)", entry.notes || "");
      if (notes === null) return;
      await updateDose(entry.id, { amount: parsed, notes });
      await refreshDoses();
    });
    row.append(" ", edit, " ", remove);
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
