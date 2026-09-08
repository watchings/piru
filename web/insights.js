export function summarizeDoses(entries, now = Date.now()) {
  const day = 24 * 60 * 60 * 1000;
  const recent = entries.filter(entry => now - entry.timestamp < 30 * day);
  const substances = new Set(recent.map(entry => entry.substance));
  const total = recent.reduce((sum, entry) => sum + entry.amount, 0);
  return {
    doseCount: recent.length,
    substanceCount: substances.size,
    totalAmount: total,
    windowDays: 30
  };
}
