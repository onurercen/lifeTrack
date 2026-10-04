import 'package:flutter/material.dart';

import '../../../core/widgets/entity_form_screen.dart';
import '../data/book_repository.dart';
import '../models/book.dart';

/// Creates a book, or edits [book] when given. Pops `true` after saving.
class BookFormScreen extends StatefulWidget {
  const BookFormScreen({super.key, required this.repository, this.book});

  final BookRepository repository;
  final Book? book;

  @override
  State<BookFormScreen> createState() => _BookFormScreenState();
}

class _BookFormScreenState extends State<BookFormScreen> {
  late final TextEditingController _title = TextEditingController(text: widget.book?.title);
  late final TextEditingController _author = TextEditingController(text: widget.book?.author);
  late final TextEditingController _description = TextEditingController(text: widget.book?.description);

  @override
  void dispose() {
    _title.dispose();
    _author.dispose();
    _description.dispose();
    super.dispose();
  }

  Future<void> _save() {
    final input = BookInput(
      title: _title.text.trim(),
      author: _author.text.trim(),
      description: _description.text.trim().isEmpty ? null : _description.text.trim(),
    );
    final book = widget.book;
    return book == null ? widget.repository.createBook(input) : widget.repository.updateBook(book.id, input);
  }

  @override
  Widget build(BuildContext context) {
    final isEditing = widget.book != null;
    return EntityFormScreen(
      title: isEditing ? 'Kitabı düzenle' : 'Yeni kitap',
      submitLabel: isEditing ? 'Kaydet' : 'Ekle',
      onSubmit: _save,
      fieldsBuilder: (context, serverErrors) => [
        FormTextField(
          controller: _title,
          label: 'Başlık',
          requiredMessage: 'Kitap başlığı zorunludur',
          icon: Icons.title,
          serverError: serverErrors['title'],
        ),
        FormTextField(
          controller: _author,
          label: 'Yazar',
          requiredMessage: 'Yazar adı zorunludur',
          icon: Icons.person_outline,
          serverError: serverErrors['author'],
        ),
        FormTextField(
          controller: _description,
          label: 'Açıklama',
          maxLength: 2000,
          maxLines: 4,
          serverError: serverErrors['description'],
        ),
      ],
    );
  }
}
