import '../../../core/utils/formatters.dart';

enum BookStatus {
  wantToRead('WANT_TO_READ', 'Okunacak'),
  reading('READING', 'Okunuyor'),
  finished('FINISHED', 'Bitti');

  const BookStatus(this.apiValue, this.label);

  final String apiValue;
  final String label;

  static BookStatus fromApi(String? value) =>
      values.firstWhere((s) => s.apiValue == value, orElse: () => BookStatus.wantToRead);
}

class Book {
  const Book({
    required this.id,
    required this.title,
    required this.author,
    this.description,
    this.status = BookStatus.wantToRead,
    this.pageCount,
    this.currentPage,
    this.rating,
    this.startedOn,
    this.finishedOn,
    required this.createdAt,
  });

  final int id;
  final String title;
  final String author;
  final String? description;
  final BookStatus status;
  final int? pageCount;
  final int? currentPage;
  final int? rating;
  final DateTime? startedOn;
  final DateTime? finishedOn;
  final DateTime createdAt;

  /// 0..1 when both page numbers are known.
  double? get progress {
    final pages = pageCount;
    final current = currentPage;
    if (pages == null || current == null || pages == 0) return null;
    return (current / pages).clamp(0, 1).toDouble();
  }

  factory Book.fromJson(Map<String, dynamic> json) {
    return Book(
      id: (json['id'] as num).toInt(),
      title: json['title'] as String,
      author: json['author'] as String,
      description: json['description'] as String?,
      status: BookStatus.fromApi(json['status'] as String?),
      pageCount: (json['pageCount'] as num?)?.toInt(),
      currentPage: (json['currentPage'] as num?)?.toInt(),
      rating: (json['rating'] as num?)?.toInt(),
      startedOn: _parseDate(json['startedOn']),
      finishedOn: _parseDate(json['finishedOn']),
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }

  static DateTime? _parseDate(Object? value) => value == null ? null : DateTime.parse(value as String);
}

class BookInput {
  const BookInput({
    required this.title,
    required this.author,
    this.description,
    this.status = BookStatus.wantToRead,
    this.pageCount,
    this.currentPage,
    this.rating,
    this.startedOn,
    this.finishedOn,
  });

  final String title;
  final String author;
  final String? description;
  final BookStatus status;
  final int? pageCount;
  final int? currentPage;
  final int? rating;
  final DateTime? startedOn;
  final DateTime? finishedOn;

  Map<String, dynamic> toJson() => {
        'title': title,
        'author': author,
        'description': description,
        'status': status.apiValue,
        'pageCount': pageCount,
        'currentPage': currentPage,
        'rating': rating,
        'startedOn': startedOn == null ? null : formatLocalDateForApi(startedOn!),
        'finishedOn': finishedOn == null ? null : formatLocalDateForApi(finishedOn!),
      };
}
