import '../../../core/utils/formatters.dart';

enum MediaStatus {
  planned('PLANNED', 'İzlenecek'),
  inProgress('IN_PROGRESS', 'İzleniyor'),
  completed('COMPLETED', 'İzlendi');

  const MediaStatus(this.apiValue, this.label);

  final String apiValue;
  final String label;

  static MediaStatus fromApi(String? value) =>
      values.firstWhere((s) => s.apiValue == value, orElse: () => MediaStatus.planned);
}

class Media {
  const Media({
    required this.id,
    required this.title,
    required this.type,
    this.url,
    this.description,
    this.status = MediaStatus.planned,
    this.rating,
    this.finishedOn,
    required this.createdAt,
  });

  /// Suggested values for [type]; the backend accepts any text.
  static const types = ['Film', 'Dizi', 'Belgesel', 'Podcast', 'Video', 'Diğer'];

  final int id;
  final String title;
  final String type;
  final String? url;
  final String? description;
  final MediaStatus status;
  final int? rating;
  final DateTime? finishedOn;
  final DateTime createdAt;

  factory Media.fromJson(Map<String, dynamic> json) {
    final finishedOn = json['finishedOn'] as String?;
    return Media(
      id: (json['id'] as num).toInt(),
      title: json['title'] as String,
      type: json['type'] as String,
      url: json['url'] as String?,
      description: json['description'] as String?,
      status: MediaStatus.fromApi(json['status'] as String?),
      rating: (json['rating'] as num?)?.toInt(),
      finishedOn: finishedOn == null ? null : DateTime.parse(finishedOn),
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }
}

class MediaInput {
  const MediaInput({
    required this.title,
    required this.type,
    this.url,
    this.description,
    this.status = MediaStatus.planned,
    this.rating,
    this.finishedOn,
  });

  final String title;
  final String type;
  final String? url;
  final String? description;
  final MediaStatus status;
  final int? rating;
  final DateTime? finishedOn;

  Map<String, dynamic> toJson() => {
        'title': title,
        'type': type,
        'url': url,
        'description': description,
        'status': status.apiValue,
        'rating': rating,
        'finishedOn': finishedOn == null ? null : formatLocalDateForApi(finishedOn!),
      };
}
