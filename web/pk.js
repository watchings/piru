const LN2 = Math.log(2);

export function keFromHalfLife(halfLifeMinutes) {
  return halfLifeMinutes > 0 ? LN2 / halfLifeMinutes : 0;
}

export function estimateKa(timeToPeakMinutes, ke) {
  if (timeToPeakMinutes <= 0 || ke <= 0) return 4 * ke;
  let ka = 4 * ke;
  for (let i = 0; i < 50; i += 1) {
    if (ka <= ke) return 4 * ke;
    const delta = ka - ke;
    const f = Math.log(ka / ke) / delta - timeToPeakMinutes;
    const derivative = (delta / ka - Math.log(ka / ke)) / (delta * delta);
    if (Math.abs(derivative) < 1e-15) break;
    const next = Math.max(ke * 1.01, ka - f / derivative);
    if (Math.abs(next - ka) < 1e-6) return next;
    ka = next;
  }
  return ka;
}

export function remainingFraction(minutes, halfLifeMinutes, timeToPeakMinutes = 60) {
  if (minutes <= 0) return 1;
  const ke = keFromHalfLife(halfLifeMinutes);
  const ka = estimateKa(timeToPeakMinutes, ke);
  if (ke <= 0 || ka <= 0) return 0;
  if (Math.abs(ka - ke) < 1e-10) return (1 + ke * minutes) * Math.exp(-ke * minutes);
  return Math.max(0, (ka * Math.exp(-ke * minutes) -
    ke * Math.exp(-ka * minutes)) / (ka - ke));
}
