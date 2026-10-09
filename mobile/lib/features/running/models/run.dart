import '../../../core/utils/formatters.dart';

/// Totals over all of the user's runs.
class RunSummary {
  const RunSummary({required this.totalCount, required this.totalDistanceKm, required this.totalDurationMinutes});

  final int totalCount;
  final double totalDistanceKm;
  final int totalDurationMinutes;

  factory RunSummary.fromJson(Map<String, dynamic> json) {
    return RunSummary(
      totalCount: (json['totalCount'] as num).toInt(),
      totalDistanceKm: (json['totalDistanceKm'] as num).toDouble(),
      totalDurationMinutes: (json['totalDurationMinutes'] as num).toInt(),
    );
  }
}

class Run {
  const Run({
    required this.id,
    required this.distanceKm,
    required this.durationMinutes,
    this.caloriesBurned,
    this.notes,
    required this.runAt,
  });

  final int id;
  final double distanceKm;
  final int durationMinutes;
  final int? caloriesBurned;
  final String? notes;

  /// When the run happened (local time, as sent by the backend).
  final DateTime runAt;

  /// Average pace in minutes per kilometre.
  double get paceMinPerKm => durationMinutes / distanceKm;

  factory Run.fromJson(Map<String, dynamic> json) {
    return Run(
      id: (json['id'] as num).toInt(),
      distanceKm: (json['distanceKm'] as num).toDouble(),
      durationMinutes: (json['durationMinutes'] as num).toInt(),
      caloriesBurned: (json['caloriesBurned'] as num?)?.toInt(),
      notes: json['notes'] as String?,
      runAt: DateTime.parse(json['runAt'] as String),
    );
  }
}

class RunInput {
  const RunInput({
    required this.distanceKm,
    required this.durationMinutes,
    this.caloriesBurned,
    this.notes,
    required this.runAt,
  });

  final double distanceKm;
  final int durationMinutes;
  final int? caloriesBurned;
  final String? notes;
  final DateTime runAt;

  Map<String, dynamic> toJson() => {
        'distanceKm': distanceKm,
        'durationMinutes': durationMinutes,
        'caloriesBurned': caloriesBurned,
        'notes': notes,
        // Backend expects a LocalDateTime without offset.
        'runAt': formatLocalDateTimeForApi(runAt),
      };
}
