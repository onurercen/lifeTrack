import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../core/widgets/entity_form_screen.dart';
import '../../../core/widgets/form_fields.dart';
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
  late final TextEditingController _pageCount = TextEditingController(text: widget.book?.pageCount?.toString());
  late final TextEditingController _currentPage = TextEditingController(text: widget.book?.currentPage?.toString());
  late BookStatus _status = widget.book?.status ?? BookStatus.wantToRead;
  late int? _rating = widget.book?.rating;
  late DateTime? _startedOn = widget.book?.startedOn;
  late DateTime? _finishedOn = widget.book?.finishedOn;

  static int? _parseInt(String text) => int.tryParse(text.trim());

  @override
  void dispose() {
    _title.dispose();
    _author.dispose();
    _description.dispose();
    _pageCount.dispose();
    _currentPage.dispose();
    super.dispose();
  }

  // Fields that don't apply to the chosen status are sent empty; the server
  // fills in today's date when a book is started or finished without one.
  Future<void> _save() {
    final description = _description.text.trim();
    final input = BookInput(
      title: _title.text.trim(),
      author: _author.text.trim(),
      description: description.isEmpty ? null : description,
      status: _status,
      pageCount: _parseInt(_pageCount.text),
      currentPage: _status == BookStatus.reading ? _parseInt(_currentPage.text) : null,
      rating: _rating,
      startedOn: _status == BookStatus.wantToRead ? null : _startedOn,
      finishedOn: _status == BookStatus.finished ? _finishedOn : null,
    );
    final book = widget.book;
    return book == null ? widget.repository.createBook(input) : widget.repository.updateBook(book.id, input);
  }

  String? _validateCurrentPage(String? value) {
    final text = value?.trim() ?? '';
    if (text.isEmpty) return null;
    final current = _parseInt(text);
    if (current == null) return 'Geçerli bir sayı giriniz';
    final pages = _parseInt(_pageCount.text);
    if (pages != null && current > pages) return 'Sayfa sayısından büyük olamaz';
    return null;
  }

  @override
  Widget build(BuildContext context) {
    final isEditing = widget.book != null;
    return EntityFormScreen(
      title: isEditing ? 'Kitabı düzenle' : 'Yeni kitap',
      submitLabel: isEditing ? 'Kaydet' : 'Ekle',
      onSubmit: _save,
      fieldsBuilder: (context, serverErrors) => [
        SegmentedButton<BookStatus>(
          segments: [
            for (final status in BookStatus.values) ButtonSegment(value: status, label: Text(status.label)),
          ],
          selected: {_status},
          showSelectedIcon: false,
          onSelectionChanged: (selection) => setState(() => _status = selection.single),
        ),
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
        _PageField(
          controller: _pageCount,
          label: 'Sayfa sayısı (opsiyonel)',
          icon: Icons.auto_stories_outlined,
          serverError: serverErrors['pageCount'],
          validator: (value) {
            final text = value?.trim() ?? '';
            if (text.isEmpty) return null;
            final pages = _parseInt(text);
            if (pages == null) return 'Geçerli bir sayı giriniz';
            if (pages < 1) return 'Sayfa sayısı en az 1 olmalıdır';
            return null;
          },
        ),
        if (_status == BookStatus.reading)
          _PageField(
            controller: _currentPage,
            label: 'Okunan sayfa (opsiyonel)',
            icon: Icons.bookmark_outline,
            serverError: serverErrors['currentPage'],
            validator: _validateCurrentPage,
          ),
        if (_status != BookStatus.wantToRead)
          FormDateField(
            label: 'Başlama tarihi',
            value: _startedOn,
            emptyText: _status == BookStatus.reading && widget.book?.status != BookStatus.reading
                ? 'Bugün'
                : 'Seçilmedi',
            icon: Icons.play_arrow_outlined,
            serverError: serverErrors['startedOn'],
            onChanged: (date) => setState(() => _startedOn = date),
          ),
        if (_status == BookStatus.finished) ...[
          FormDateField(
            label: 'Bitirme tarihi',
            value: _finishedOn,
            emptyText: widget.book?.status != BookStatus.finished ? 'Bugün' : 'Seçilmedi',
            icon: Icons.flag_outlined,
            serverError: serverErrors['finishedOn'],
            onChanged: (date) => setState(() => _finishedOn = date),
          ),
          RatingField(
            value: _rating,
            serverError: serverErrors['rating'],
            onChanged: (rating) => setState(() => _rating = rating),
          ),
        ],
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

class _PageField extends StatelessWidget {
  const _PageField({
    required this.controller,
    required this.label,
    required this.icon,
    required this.validator,
    this.serverError,
  });

  final TextEditingController controller;
  final String label;
  final IconData icon;
  final FormFieldValidator<String> validator;
  final String? serverError;

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      controller: controller,
      keyboardType: TextInputType.number,
      inputFormatters: [FilteringTextInputFormatter.digitsOnly],
      textInputAction: TextInputAction.next,
      decoration: InputDecoration(
        labelText: label,
        suffixText: 'sayfa',
        prefixIcon: Icon(icon),
        border: const OutlineInputBorder(),
        errorText: serverError,
      ),
      validator: validator,
    );
  }
}
