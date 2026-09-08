import 'package:flutter_test/flutter_test.dart';
import 'package:piru_flutter/models/dose_entry.dart';
import 'package:piru_flutter/models/route_of_administration.dart';

void main() {
  test('parses route aliases like the Swift model', () {
    expect(RouteOfAdministrationCodec.parse('intranasal'), RouteOfAdministration.insufflation);
    expect(RouteOfAdministrationCodec.parse('pouch'), RouteOfAdministration.buccal);
    expect(RouteOfAdministrationCodec.parse('unknown'), RouteOfAdministration.other);
  });

  test('clamps negative doses and round-trips portable fields', () {
    final dose = DoseEntry(
      substance: 'Caffeine',
      amount: -1,
      route: RouteOfAdministration.oral,
      tags: const ['morning'],
    );
    expect(dose.amount, 0);
    expect(dose.tags, ['morning']);
    expect(dose.toJson()['timestamp'], isA<int>());
    expect(dose.toJson()['route'], 'oral');
  });
}
