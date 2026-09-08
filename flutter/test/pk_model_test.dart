import 'package:flutter_test/flutter_test.dart';
import 'package:piru_flutter/engines/pk_model.dart';

void main() {
  group('PKModel analytic functions', () {
    test('concentration handles invalid inputs and ka ~= ke', () {
      expect(
        PKModel.concentration(minutes: -1, ke: 0.1, ka: 0.4),
        0,
      );
      expect(
        PKModel.concentration(minutes: 10, ke: 0.1, ka: 0.1),
        closeTo(0.1 * 10 * 0.36787944117, 1e-10),
      );
    });

    test('absolute and molar concentration preserve scale', () {
      final shape = PKModel.concentration(minutes: 20, ke: 0.05, ka: 0.2);
      final absolute = PKModel.concentrationAbsolute(
        dose: 120,
        bioavailability: 0.5,
        vdPerKg: 1,
        weightKg: 60,
        ke: 0.05,
        ka: 0.2,
        minutes: 20,
      );
      expect(absolute, closeTo(shape, 1e-12));
      expect(
        PKModel.concentrationMolar(
          dose: 120,
          bioavailability: 0.5,
          vdPerKg: 1,
          weightKg: 60,
          molarMassGramsPerMole: 120,
          ke: 0.05,
          ka: 0.2,
          minutes: 20,
        ),
        closeTo(shape / 120000, 1e-12),
      );
    });

    test('occupancy supports mass action and Hill coefficients', () {
      expect(
        PKModel.occupancy(concentration: 10, halfMax: 10),
        closeTo(0.5, 1e-12),
      );
      expect(
        PKModel.occupancy(
          concentration: 10,
          halfMax: 10,
          hillCoefficient: 2,
        ),
        closeTo(0.5, 1e-12),
      );
    });

    test('tmax, cmax, half-life and ka estimation agree', () {
      final ke = PKModel.keFromHalfLifeMinutes(10);
      final ka = PKModel.estimateKa(timeToPeak: 10, ke: ke);
      expect(PKModel.tmax(ke: ke, ka: ka), closeTo(10, 1e-6));
      expect(PKModel.cmax(ke: ke, ka: ka), greaterThan(0));
      expect(PKModel.defaultKa(ke: ke), 4 * ke);
    });

    test('body fraction and descending fraction time are bounded', () {
      expect(
        PKModel.fractionRemainingInBody(minutes: 0, ke: 0.1, ka: 0.4),
        1,
      );
      expect(
        PKModel.fractionRemainingInBody(minutes: 100, ke: 0.1, ka: 0.4),
        closeTo(0, 1e-8),
      );
      expect(
        PKModel.timeToFraction(0.5, ke: 0.1, ka: 0.4),
        greaterThan(PKModel.tmax(ke: 0.1, ka: 0.4)),
      );
    });
  });

  group('PKModel saturable functions', () {
    const elimination = SaturationElimination(km: 1, vmax: 0.1);
    const activation = SaturationActivation(
      km: 1,
      vmax: 0.1,
      fractionConverted: 0.8,
      parentEliminationKe: 0.02,
      metaboliteKe: 0.03,
    );

    test('RK4 curves have expected shape and effect species', () {
      final parentCurve = PKModel.saturableCurve(
        dose: 100,
        bioavailability: 0.8,
        vdPerKg: 1,
        weightKg: 60,
        ka: 0.2,
        saturation: elimination,
        durationMinutes: 30,
      );
      expect(parentCurve.parent.length, 31);
      expect(parentCurve.metabolite, isNull);
      expect(parentCurve.peakParent, greaterThan(0));
      expect(parentCurve.effectAUC, greaterThan(0));

      final activationCurve = PKModel.saturableCurve(
        dose: 100,
        bioavailability: 0.8,
        vdPerKg: 1,
        weightKg: 60,
        ka: 0.2,
        saturation: activation,
        durationMinutes: 30,
      );
      expect(activationCurve.metabolite, isNotNull);
      expect(activationCurve.effect, same(activationCurve.metabolite));
    });

    test('zero-order kinetics scale and produce a bounded shape', () {
      final kinetics = PKModel.zeroOrderKinetics(
        vmaxMgPerMin: 1,
        referenceWeightKg: 60,
        kaPerMin: 0.2,
        bioavailability: 1,
        weightKg: 60,
      );
      expect(kinetics.vmaxMgPerMin, 1);
      final peak = PKModel.zeroOrderPeakMinutes(
        doseMg: 100,
        kinetics: kinetics,
      );
      expect(peak, greaterThan(0));
      expect(
        PKModel.zeroOrderBodyContent(
          doseMg: 100,
          minutes: 0,
          kinetics: kinetics,
        ),
        0,
      );
      expect(
        PKModel.zeroOrderShape(
          doseMg: 100,
          minutes: peak,
          kinetics: kinetics,
        ),
        closeTo(1, 1e-12),
      );
      expect(
        PKModel.zeroOrderClearMinutes(doseMg: 100, kinetics: kinetics),
        greaterThan(peak),
      );
    });
  });
}
