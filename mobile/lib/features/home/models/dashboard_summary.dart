class DashboardSummary {
  const DashboardSummary({
    required this.totalRunCount,
    required this.totalDistanceKm,
    required this.weekRunCount,
    required this.weekDistanceKm,
    required this.weekDurationMinutes,
    required this.lastSevenDays,
    required this.books,
    required this.media,
  });

  final int totalRunCount;
  final double totalDistanceKm;
  final int weekRunCount;
  final double weekDistanceKm;
  final int weekDurationMinutes;
  final List<DailyDistance> lastSevenDays;
  final BookStats books;
  final MediaStats media;

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
      books: BookStats.fromJson(json['books'] as Map<String, dynamic>),
      media: MediaStats.fromJson(json['media'] as Map<String, dynamic>),
    );
  }
}

class BookStats {
  const BookStats({
    required this.totalCount,
    required this.readingCount,
    required this.finishedThisYear,
    required this.currentlyReading,
  });

  final int totalCount;
  final int readingCount;
  final int finishedThisYear;
  final List<ReadingBook> currentlyReading;

  factory BookStats.fromJson(Map<String, dynamic> json) => BookStats(
        totalCount: (json['totalCount'] as num).toInt(),
        readingCount: (json['readingCount'] as num).toInt(),
        finishedThisYear: (json['finishedThisYear'] as num).toInt(),
        currentlyReading: (json['currentlyReading'] as List<dynamic>)
            .map((e) => ReadingBook.fromJson(e as Map<String, dynamic>))
            .toList(),
      );
}

class ReadingBook {
  const ReadingBook({required this.id, required this.title, required this.author, this.currentPage, this.pageCount});

  final int id;
  final String title;
  final String author;
  final int? currentPage;
  final int? pageCount;

  /// 0..1 when both page numbers are known.
  double? get progress {
    final pages = pageCount;
    final current = currentPage;
    if (pages == null || current == null || pages == 0) return null;
    return (current / pages).clamp(0, 1).toDouble();
  }

  factory ReadingBook.fromJson(Map<String, dynamic> json) => ReadingBook(
        id: (json['id'] as num).toInt(),
        title: json['title'] as String,
        author: json['author'] as String,
        currentPage: (json['currentPage'] as num?)?.toInt(),
        pageCount: (json['pageCount'] as num?)?.toInt(),
      );
}

class MediaStats {
  const MediaStats({required this.totalCount, required this.inProgressCount, required this.completedThisYear});

  final int totalCount;
  final int inProgressCount;
  final int completedThisYear;

  factory MediaStats.fromJson(Map<String, dynamic> json) => MediaStats(
        totalCount: (json['totalCount'] as num).toInt(),
        inProgressCount: (json['inProgressCount'] as num).toInt(),
        completedThisYear: (json['completedThisYear'] as num).toInt(),
      );
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
