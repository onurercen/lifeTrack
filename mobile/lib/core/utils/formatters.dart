const _months = ['Oca', 'Şub', 'Mar', 'Nis', 'May', 'Haz', 'Tem', 'Ağu', 'Eyl', 'Eki', 'Kas', 'Ara'];

String _two(int n) => n.toString().padLeft(2, '0');

/// e.g. "2026-10-04T07:30:00" — local time, no offset, for LocalDateTime fields.
String formatLocalDateTimeForApi(DateTime value) {
  final v = value.isUtc ? value.toLocal() : value;
  return '${v.year.toString().padLeft(4, '0')}-${_two(v.month)}-${_two(v.day)}'
      'T${_two(v.hour)}:${_two(v.minute)}:${_two(v.second)}';
}

/// e.g. "4 Eki 2026, 07:30"
String formatDateTime(DateTime value) {
  final local = value.toLocal();
  return '${local.day} ${_months[local.month - 1]} ${local.year}, ${_two(local.hour)}:${_two(local.minute)}';
}

/// e.g. "5,2" — Turkish decimal comma, trailing zeros trimmed.
String formatDecimal(double value, {int fractionDigits = 1}) {
  var text = value.toStringAsFixed(fractionDigits);
  if (text.contains('.')) {
    text = text.replaceFirst(RegExp(r'0+$'), '').replaceFirst(RegExp(r'\.$'), '');
  }
  return text.replaceAll('.', ',');
}

/// e.g. 95 -> "1 sa 35 dk", 30 -> "30 dk"
String formatDuration(int minutes) {
  final h = minutes ~/ 60;
  final m = minutes % 60;
  if (h == 0) return '$m dk';
  return m == 0 ? '$h sa' : '$h sa $m dk';
}

/// Pace in min/km, e.g. 5.75 -> "5:45 /km"
String formatPace(double minPerKm) {
  final totalSeconds = (minPerKm * 60).round();
  return '${totalSeconds ~/ 60}:${_two(totalSeconds % 60)} /km';
}

const _weekdays = ['Pzt', 'Sal', 'Çar', 'Per', 'Cum', 'Cmt', 'Paz'];

/// e.g. "Pzt"
String formatWeekdayShort(DateTime value) => _weekdays[value.weekday - 1];
