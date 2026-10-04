class Media {
  const Media({
    required this.id,
    required this.title,
    required this.type,
    this.url,
    this.description,
    required this.createdAt,
  });

  /// Suggested values for [type]; the backend accepts any text.
  static const types = ['Film', 'Dizi', 'Belgesel', 'Podcast', 'Video', 'Diğer'];

  final int id;
  final String title;
  final String type;
  final String? url;
  final String? description;
  final DateTime createdAt;

  factory Media.fromJson(Map<String, dynamic> json) {
    return Media(
      id: (json['id'] as num).toInt(),
      title: json['title'] as String,
      type: json['type'] as String,
      url: json['url'] as String?,
      description: json['description'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }
}

class MediaInput {
  const MediaInput({required this.title, required this.type, this.url, this.description});

  final String title;
  final String type;
  final String? url;
  final String? description;

  Map<String, dynamic> toJson() => {'title': title, 'type': type, 'url': url, 'description': description};
}
