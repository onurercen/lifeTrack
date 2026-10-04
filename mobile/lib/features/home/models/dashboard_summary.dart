class DashboardSummary {
  const DashboardSummary({
    required this.totalRunCount,
    required this.totalDistanceKm,
    required this.weekRunCount,
    required this.weekDistanceKm,
    required this.weekDurationMinutes,
    required this.lastSevenDays,
    required this.bookCount,
    required this.mediaCount,
  });

  final int totalRunCount;
  final double totalDistanceKm;
  final int weekRunCount;
  final double weekDistanceKm;
  final int weekDurationMinutes;
  final List<DailyDistance> lastSevenDays;
  final int bookCount;
  final int mediaCount;

  factory DashboardSummary.fromJson(Map<String, dynamic> json) {
    final runs = json['runs'] as Map<String, dynamic>;
    return DashboardSummary(
      totalRunCount: (runs['totalCount'] as num).toInt(),
      totalDistanceKm: (runs['totalDistanceKm'] as num).toDouble(),
      weekRunCount: (runs['weekCount'] as num).toInt(),
      weekDistanceKm: (runs['weekDistanceKm'] as num).toDouble(),
      weekDurationMinutes: (runs['weekDurationMinutes'] as num).toInt(),
      lastSevenDays: (runs['lastSevenDays'] as List<dynamic>)
          .map((e) => DailyDistance.fromJson(e as Map<String, dynamic>))
          .toList(),
      bookCount: (json['bookCount'] as num).toInt(),
      mediaCount: (json['mediaCount'] as num).toInt(),
    );
  }
}

class DailyDistance {
  const DailyDistance({required this.date, required this.distanceKm});

  final DateTime date;
  final double distanceKm;

  factory DailyDistance.fromJson(Map<String, dynamic> json) => DailyDistance(
        date: DateTime.parse(json['date'] as String),
        distanceKm: (json['distanceKm'] as num).toDouble(),
      );
}
