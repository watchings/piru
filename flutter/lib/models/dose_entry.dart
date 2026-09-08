import 'route_of_administration.dart';

class DoseEntry {
  DoseEntry({
    required this.substance,
    required double amount,
    this.unit = 'mg',
    this.route = RouteOfAdministration.oral,
    this.saltForm,
    this.isomer,
    this.releaseForm,
    this.productName,
    this.substanceUid,
    this.displayNameSnapshot,
    DateTime? timestamp,
    this.notes,
    List<String> tags = const [],
    this.isBackgroundMed = false,
    this.locationName,
    this.latitude,
    this.longitude,
    this.hadGrapefruit,
    this.isApproximate = false,
    this.volumeMl,
    this.abv,
    this.drinkName,
    String? id,
  })  : id = id ?? _newId(),
        amount = amount < 0 ? 0 : amount,
        timestamp = timestamp ?? DateTime.now(),
        _tags = List.unmodifiable(tags);

  final String id;
  final String substance;
  final double amount;
  final String unit;
  final RouteOfAdministration route;
  final String? saltForm;
  final String? isomer;
  final String? releaseForm;
  final String? productName;
  final String? substanceUid;
  final String? displayNameSnapshot;
  final DateTime timestamp;
  final String? notes;
  final List<String> _tags;
  final bool isBackgroundMed;
  final String? locationName;
  final double? latitude;
  final double? longitude;
  final bool? hadGrapefruit;
  final bool isApproximate;
  final double? volumeMl;
  final double? abv;
  final String? drinkName;

  List<String> get tags => _tags;

  String get identityKey {
    final identity = substanceUid ?? substance.toLowerCase();
    return [identity, isomer, releaseForm, saltForm].whereType<String>().join('|');
  }

  Map<String, Object?> toJson() => {
        'id': id,
        'substance': substance,
        'amount': amount,
        'unit': unit,
        'route': route.rawValue,
        'saltForm': saltForm,
        'isomer': isomer,
        'releaseForm': releaseForm,
        'productName': productName,
        'substanceUID': substanceUid,
        'displayNameSnapshot': displayNameSnapshot,
        'timestamp': timestamp.millisecondsSinceEpoch,
        'notes': notes,
        'tags': tags,
        'isBackgroundMed': isBackgroundMed,
        'locationName': locationName,
        'latitude': latitude,
        'longitude': longitude,
        'hadGrapefruit': hadGrapefruit,
        'isApproximate': isApproximate,
        'volumeML': volumeMl,
        'abv': abv,
        'drinkName': drinkName,
      };

  static String _newId() => DateTime.now().microsecondsSinceEpoch.toString();
}
