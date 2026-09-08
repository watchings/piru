import 'dart:math' as math;

/// Pure pharmacokinetic calculations used by Piru.
///
/// All times are in minutes, rate constants are per minute, and concentrations
/// are in the units stated by each method.
abstract final class PKModel {
  static double concentration({
    required double minutes,
    required double ke,
    required double ka,
  }) {
    if (minutes < 0 || ka <= 0 || ke <= 0) return 0;
    if ((ka - ke).abs() < 1e-10) {
      return ke * minutes * math.exp(-ke * minutes);
    }
    final raw = (ka / (ka - ke)) *
        (math.exp(-ke * minutes) - math.exp(-ka * minutes));
    return math.max(0, raw).toDouble();
  }

  static double concentrationAbsolute({
    required double dose,
    required double bioavailability,
    required double vdPerKg,
    required double weightKg,
    required double ke,
    required double ka,
    required double minutes,
  }) {
    if (dose < 0 ||
        bioavailability <= 0 ||
        vdPerKg <= 0 ||
        weightKg <= 0) {
      return 0;
    }
    final vd = vdPerKg * weightKg;
    return (bioavailability * dose / vd) *
        concentration(minutes: minutes, ke: ke, ka: ka);
  }

  static double concentrationMolar({
    required double dose,
    required double bioavailability,
    required double vdPerKg,
    required double weightKg,
    required double molarMassGramsPerMole,
    required double ke,
    required double ka,
    required double minutes,
  }) {
    if (molarMassGramsPerMole <= 0) return 0;
    final massPerLiter = concentrationAbsolute(
      dose: dose,
      bioavailability: bioavailability,
      vdPerKg: vdPerKg,
      weightKg: weightKg,
      ke: ke,
      ka: ka,
      minutes: minutes,
    );
    return massPerLiter / 1000 / molarMassGramsPerMole;
  }

  static double occupancy({
    required double concentration,
    required double halfMax,
    double hillCoefficient = 1,
  }) {
    if (concentration <= 0 || halfMax <= 0 || hillCoefficient <= 0) return 0;
    if (hillCoefficient == 1) {
      return concentration / (halfMax + concentration);
    }
    final cH = math.pow(concentration, hillCoefficient).toDouble();
    final kH = math.pow(halfMax, hillCoefficient).toDouble();
    return cH / (kH + cH);
  }

  static double tmax({required double ke, required double ka}) {
    if (ka <= ke || ka <= 0 || ke <= 0) return 0;
    if ((ka - ke).abs() < 1e-10) return 1 / ke;
    return math.log(ka / ke) / (ka - ke);
  }

  static double cmax({required double ke, required double ka}) =>
      concentration(minutes: tmax(ke: ke, ka: ka), ke: ke, ka: ka);

  static double keFromHalfLifeMinutes(double halfLifeMinutes) =>
      halfLifeMinutes > 0 ? math.log(2) / halfLifeMinutes : 0;

  static double estimateKa({
    required double timeToPeak,
    required double ke,
  }) {
    if (timeToPeak <= 0 || ke <= 0) return defaultKa(ke: ke);
    var ka = 4 * ke;
    for (var i = 0; i < 50; i++) {
      if (ka <= ke) return defaultKa(ke: ke);
      final f = math.log(ka / ke) / (ka - ke) - timeToPeak;
      final denominator = (ka - ke) * (ka - ke);
      final df =
          ((ka - ke) / ka - math.log(ka / ke)) / denominator;
      if (df.abs() <= 1e-15) break;
      final kaNew = ka - f / df;
      if ((kaNew - ka).abs() < 1e-6) {
        ka = math.max(ke * 1.01, kaNew).toDouble();
        break;
      }
      ka = math.max(ke * 1.01, kaNew).toDouble();
    }
    return ka;
  }

  static double defaultKa({required double ke}) => 4 * ke;

  static double fractionRemainingInBody({
    required double minutes,
    required double ke,
    required double ka,
  }) {
    if (minutes < 0 || ka <= 0 || ke <= 0) return 1;
    if ((ka - ke).abs() < 1e-10) {
      return (1 + ke * minutes) * math.exp(-ke * minutes);
    }
    final result = (ka * math.exp(-ke * minutes) -
            ke * math.exp(-ka * minutes)) /
        (ka - ke);
    return math.min(1, math.max(0, result)).toDouble();
  }

  static double timeToFraction(
    double fraction, {
    required double ke,
    required double ka,
    double maxMinutes = 50000,
  }) {
    final peak = cmax(ke: ke, ka: ka);
    if (peak <= 0) return 0;
    final target = peak * fraction;
    var lo = tmax(ke: ke, ka: ka);
    var hi = maxMinutes;
    for (var i = 0; i < 100; i++) {
      final mid = (lo + hi) / 2;
      if (concentration(minutes: mid, ke: ke, ka: ka) > target) {
        lo = mid;
      } else {
        hi = mid;
      }
      if (hi - lo < 0.1) break;
    }
    return hi;
  }

  static const referenceBodyWeightKg = 60.0;
  static const minimumModeledWeightKg = 20.0;
  static const maximumModeledWeightKg = 300.0;

  static ZeroOrderKinetics zeroOrderKinetics({
    required double vmaxMgPerMin,
    required double referenceWeightKg,
    required double kaPerMin,
    required double bioavailability,
    required double weightKg,
  }) {
    final clamped = weightKg.isFinite
        ? math.min(
            math.max(weightKg, minimumModeledWeightKg),
            maximumModeledWeightKg,
          ).toDouble()
        : referenceBodyWeightKg;
    final reference =
        referenceWeightKg > 0 ? referenceWeightKg : referenceBodyWeightKg;
    return ZeroOrderKinetics(
      bioavailability: bioavailability,
      vmaxMgPerMin: vmaxMgPerMin / reference * clamped,
      ka: kaPerMin,
    );
  }

  static double zeroOrderBodyContent({
    required double doseMg,
    required double minutes,
    required ZeroOrderKinetics kinetics,
  }) {
    final k = kinetics;
    if (doseMg <= 0 ||
        minutes < 0 ||
        k.bioavailability <= 0 ||
        k.vmaxMgPerMin <= 0 ||
        k.ka <= 0) {
      return 0;
    }
    final fd = k.bioavailability * doseMg;
    final absorbed = fd * (1 - math.exp(-k.ka * minutes));
    return math.max(0, absorbed - k.vmaxMgPerMin * minutes).toDouble();
  }

  static double zeroOrderPeakMinutes({
    required double doseMg,
    required ZeroOrderKinetics kinetics,
  }) {
    final k = kinetics;
    if (doseMg <= 0 || k.ka <= 0 || k.bioavailability <= 0) return 0;
    final fd = k.bioavailability * doseMg;
    final ratio = k.vmaxMgPerMin / (fd * k.ka);
    if (ratio <= 0 || ratio >= 1) return 0;
    return -math.log(ratio) / k.ka;
  }

  static double zeroOrderClearMinutes({
    required double doseMg,
    required ZeroOrderKinetics kinetics,
  }) {
    final peak = zeroOrderPeakMinutes(doseMg: doseMg, kinetics: kinetics);
    if (peak <= 0) return 0;
    var lo = peak;
    var hi = peak +
        kinetics.bioavailability * doseMg / kinetics.vmaxMgPerMin +
        60;
    for (var i = 0; i < 60; i++) {
      final mid = (lo + hi) / 2;
      if (zeroOrderBodyContent(
            doseMg: doseMg,
            minutes: mid,
            kinetics: kinetics,
          ) >
          0) {
        lo = mid;
      } else {
        hi = mid;
      }
    }
    return hi;
  }

  static double? zeroOrderShape({
    required double doseMg,
    required double minutes,
    required ZeroOrderKinetics kinetics,
  }) {
    if (minutes < 0) return null;
    final peakT =
        zeroOrderPeakMinutes(doseMg: doseMg, kinetics: kinetics);
    if (peakT <= 0) return null;
    final peak = zeroOrderBodyContent(
      doseMg: doseMg,
      minutes: peakT,
      kinetics: kinetics,
    );
    if (peak <= 0) return null;
    return math.min(
      1,
      math.max(
        0,
        zeroOrderBodyContent(
              doseMg: doseMg,
              minutes: minutes,
              kinetics: kinetics,
            ) /
            peak,
      ),
    ).toDouble();
  }

  static SaturableCurve saturableCurve({
    required double dose,
    required double bioavailability,
    required double vdPerKg,
    required double weightKg,
    required double ka,
    required Saturation saturation,
    required double durationMinutes,
    double stepMinutes = 1,
  }) {
    if (dose <= 0 ||
        bioavailability <= 0 ||
        vdPerKg <= 0 ||
        weightKg <= 0 ||
        ka <= 0 ||
        durationMinutes <= 0 ||
        stepMinutes <= 0 ||
        saturation is SaturationNone) {
      return SaturableCurve(
        stepMinutes: stepMinutes,
        parent: const [0],
        metabolite: null,
      );
    }
    final scale = bioavailability * dose / (vdPerKg * weightKg);
    final steps = math.max(1, (durationMinutes / stepMinutes).round()).toInt();
    final h = stepMinutes;
    final tracksMetabolite = saturation is SaturationActivation;

    List<double> derivative(List<double> state) {
      final g = state[0];
      final c = math.max(0, state[1]).toDouble();
      final absorptionFlux = ka * g * scale;
      if (saturation case SaturationElimination(:final km, :final vmax)) {
        final elimination = vmax * c / (km + c);
        return [-ka * g, absorptionFlux - elimination];
      }
      if (saturation case SaturationActivation(
            :final km,
            :final vmax,
            :final fractionConverted,
            :final parentEliminationKe,
            :final metaboliteKe,
          )) {
        final m = math.max(0, state[2]).toDouble();
        final formation = vmax * c / (km + c);
        return [
          -ka * g,
          absorptionFlux - parentEliminationKe * c - formation,
          fractionConverted * formation - metaboliteKe * m,
        ];
      }
      return [-ka * g, 0];
    }

    List<double> rk4Step(List<double> state) {
      final k1 = derivative(state);
      final k2 = derivative([
        for (var i = 0; i < state.length; i++) state[i] + 0.5 * h * k1[i],
      ]);
      final k3 = derivative([
        for (var i = 0; i < state.length; i++) state[i] + 0.5 * h * k2[i],
      ]);
      final k4 = derivative([
        for (var i = 0; i < state.length; i++) state[i] + h * k3[i],
      ]);
      return [
        for (var i = 0; i < state.length; i++)
          math.max(
            0,
            state[i] + h / 6 * (k1[i] + 2 * k2[i] + 2 * k3[i] + k4[i]),
          ).toDouble(),
      ];
    }

    var state = tracksMetabolite ? <double>[1, 0, 0] : <double>[1, 0];
    final parent = <double>[0];
    final metabolite = tracksMetabolite ? <double>[0] : null;
    for (var i = 0; i < steps; i++) {
      state = rk4Step(state);
      parent.add(state[1]);
      metabolite?.add(state[2]);
    }
    return SaturableCurve(
      stepMinutes: h,
      parent: parent,
      metabolite: metabolite,
    );
  }
}

sealed class Saturation {
  const Saturation();
}

final class SaturationNone extends Saturation {
  const SaturationNone();
}

final class SaturationElimination extends Saturation {
  const SaturationElimination({required this.km, required this.vmax});
  final double km;
  final double vmax;
}

final class SaturationActivation extends Saturation {
  const SaturationActivation({
    required this.km,
    required this.vmax,
    required this.fractionConverted,
    required this.parentEliminationKe,
    required this.metaboliteKe,
  });
  final double km;
  final double vmax;
  final double fractionConverted;
  final double parentEliminationKe;
  final double metaboliteKe;
}

final class SaturableCurve {
  const SaturableCurve({
    required this.stepMinutes,
    required this.parent,
    required this.metabolite,
  });

  final double stepMinutes;
  final List<double> parent;
  final List<double>? metabolite;

  List<double> get effect => metabolite ?? parent;
  double get peakParent => parent.isEmpty
      ? 0
      : parent.fold<double>(0, (peak, value) => math.max(peak, value).toDouble());
  double get peakEffect => effect.isEmpty
      ? 0
      : effect.fold<double>(0, (peak, value) => math.max(peak, value).toDouble());

  double get effectAUC {
    if (effect.length <= 1) return 0;
    var sum = 0.0;
    for (var i = 1; i < effect.length; i++) {
      sum += (effect[i] + effect[i - 1]) / 2 * stepMinutes;
    }
    return sum;
  }
}

final class ZeroOrderKinetics {
  const ZeroOrderKinetics({
    required this.bioavailability,
    required this.vmaxMgPerMin,
    required this.ka,
  });

  final double bioavailability;
  final double vmaxMgPerMin;
  final double ka;
}
