import 'package:flutter/material.dart';

import '../../../app/app_scope.dart';
import '../../../core/widgets/entity_list_screen.dart';
import '../../../core/widgets/form_fields.dart';
import '../data/book_repository.dart';
import '../models/book.dart';
import 'book_form_screen.dart';

class BooksScreen extends StatelessWidget {
  const BooksScreen({super.key});

  static IconData iconFor(BookStatus status) => switch (status) {
        BookStatus.wantToRead => Icons.bookmark_add_outlined,
        BookStatus.reading => Icons.auto_stories,
        BookStatus.finished => Icons.task_alt,
      };

  @override
  Widget build(BuildContext context) {
    final repository = BookRepository(AppScope.read(context).api);
    return EntityListScreen<Book>(
      title: 'Kitaplar',
      addLabel: 'Kitap ekle',
      emptyIcon: Icons.menu_book,
      emptyText: 'Henüz kitap eklemedin.\nOkuduğun ya da okumak istediğin kitapları ekle.',
      searchHint: 'Başlık, yazar veya açıklamada ara',
      filters: [
        const ListFilter('Tümü', null),
        for (final status in BookStatus.values) ListFilter(status.label, status.apiValue),
      ],
      load: (query, filter, page) => repository.fetchBooks(
        query: query,
        status: filter == null ? null : BookStatus.fromApi(filter),
        page: page,
      ),
      delete: (book) => repository.deleteBook(book.id),
      idOf: (book) => book.id,
      deletePrompt: (book) => '"${book.title}" silinsin mi?',
      itemBuilder: (context, book, onTap) => _BookTile(book: book, onTap: onTap),
      formBuilder: (book) => BookFormScreen(repository: repository, book: book),
    );
  }
}

class _BookTile extends StatelessWidget {
  const _BookTile({required this.book, required this.onTap});

  final Book book;
  final VoidCallback onTap;

  String get _statusLine {
    final parts = <String>[book.status.label];
    if (book.status == BookStatus.reading && book.currentPage != null) {
      parts.add(book.pageCount == null ? '${book.currentPage}. sayfa' : '${book.currentPage}/${book.pageCount} sayfa');
    } else if (book.pageCount != null) {
      parts.add('${book.pageCount} sayfa');
    }
    if (book.rating != null) parts.add(formatStars(book.rating!));
    return parts.join(' · ');
  }

  @override
  Widget build(BuildContext context) {
    final progress = book.status == BookStatus.reading ? book.progress : null;
    return ListTile(
      onTap: onTap,
      leading: CircleAvatar(child: Icon(BooksScreen.iconFor(book.status))),
      title: Text(book.title),
      isThreeLine: true,
      subtitle: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(book.author, maxLines: 1, overflow: TextOverflow.ellipsis),
          Text(_statusLine, maxLines: 1, overflow: TextOverflow.ellipsis),
          if (progress != null)
            Padding(
              padding: const EdgeInsets.only(top: 6),
              child: LinearProgressIndicator(value: progress, borderRadius: BorderRadius.circular(2)),
            ),
        ],
      ),
    );
  }
}
