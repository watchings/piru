enum RouteOfAdministration {
  oral,
  sublingual,
  buccal,
  insufflation,
  inhalation,
  intravenous,
  intramuscular,
  subcutaneous,
  transdermal,
  rectal,
  other,
}

extension RouteOfAdministrationCodec on RouteOfAdministration {
  String get rawValue => name;

  String get displayName => switch (this) {
        RouteOfAdministration.oral => 'Oral',
        RouteOfAdministration.sublingual => 'Sublingual',
        RouteOfAdministration.buccal => 'Buccal',
        RouteOfAdministration.insufflation => 'Insufflation',
        RouteOfAdministration.inhalation => 'Inhalation',
        RouteOfAdministration.intravenous => 'Intravenous',
        RouteOfAdministration.intramuscular => 'Intramuscular',
        RouteOfAdministration.subcutaneous => 'Subcutaneous',
        RouteOfAdministration.transdermal => 'Transdermal',
        RouteOfAdministration.rectal => 'Rectal',
        RouteOfAdministration.other => 'Other',
      };

  static RouteOfAdministration parse(String value) {
    return switch (value.trim().toLowerCase()) {
      'oral' || 'oral_ir' || 'oral_er' || 'oral(benzedrex)' || 'oral(pure)' =>
        RouteOfAdministration.oral,
      'sublingual' => RouteOfAdministration.sublingual,
      'buccal' || 'buccally' || 'pouch' || 'snus' => RouteOfAdministration.buccal,
      'insufflated' || 'insufflation' || 'insufflated(pure)' || 'intranasal' || 'nasal' =>
        RouteOfAdministration.insufflation,
      'inhaled' || 'inhalation' || 'smoked' || 'vapourized' || 'vaporized' =>
        RouteOfAdministration.inhalation,
      'intravenous' || 'iv' => RouteOfAdministration.intravenous,
      'intramuscular' || 'im' => RouteOfAdministration.intramuscular,
      'subcutaneous' => RouteOfAdministration.subcutaneous,
      'transdermal' || 'topical' => RouteOfAdministration.transdermal,
      'rectal' || 'plugged' => RouteOfAdministration.rectal,
      _ => RouteOfAdministration.other,
    };
  }
}
