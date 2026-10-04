class Book {
  const Book({
    required this.id,
    required this.title,
    required this.author,
    this.description,
    required this.createdAt,
  });

  final int id;
  final String title;
  final String author;
  final String? description;
  final DateTime createdAt;

  factory Book.fromJson(Map<String, dynamic> json) {
    return Book(
      id: (json['id'] as num).toInt(),
      title: json['title'] as String,
      author: json['author'] as String,
      description: json['description'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }
}

class BookInput {
  const BookInput({required this.title, required this.author, this.description});

  final String title;
  final String author;
  final String? description;

  Map<String, dynamic> toJson() => {'title': title, 'author': author, 'description': description};
}
