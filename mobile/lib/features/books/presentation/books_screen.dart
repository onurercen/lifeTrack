import 'package:flutter/material.dart';

import '../../../app/app_scope.dart';
import '../../../core/widgets/entity_list_screen.dart';
import '../data/book_repository.dart';
import '../models/book.dart';
import 'book_form_screen.dart';

class BooksScreen extends StatelessWidget {
  const BooksScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final repository = BookRepository(AppScope.read(context).api);
    return EntityListScreen<Book>(
      title: 'Kitaplar',
      addLabel: 'Kitap ekle',
      emptyIcon: Icons.menu_book,
      emptyText: 'Henüz kitap eklemedin.\nOkuduğun ya da okumak istediğin kitapları ekle.',
      searchHint: 'Başlık, yazar veya açıklamada ara',
      load: (query) => repository.fetchBooks(query: query),
      delete: (book) => repository.deleteBook(book.id),
      idOf: (book) => book.id,
      deletePrompt: (book) => '"${book.title}" silinsin mi?',
      itemBuilder: (context, book, onTap) => ListTile(
        onTap: onTap,
        leading: const CircleAvatar(child: Icon(Icons.menu_book)),
        title: Text(book.title),
        subtitle: Text(
          [book.author, if (book.description != null) book.description!].join('\n'),
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
        ),
        isThreeLine: book.description != null,
      ),
      formBuilder: (book) => BookFormScreen(repository: repository, book: book),
    );
  }
}
