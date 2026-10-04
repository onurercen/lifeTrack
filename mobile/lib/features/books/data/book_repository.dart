import '../../../core/network/api_client.dart';
import '../models/book.dart';

class BookRepository {
  const BookRepository(this._api);

  final ApiClient _api;

  Future<List<Book>> fetchBooks({String? query}) async {
    final json = await _api.get('books', query: query == null ? null : {'query': query}) as List<dynamic>;
    return json.map((e) => Book.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<Book> createBook(BookInput input) async {
    final json = await _api.post('books', body: input.toJson());
    return Book.fromJson(json as Map<String, dynamic>);
  }

  Future<Book> updateBook(int id, BookInput input) async {
    final json = await _api.put('books/$id', body: input.toJson());
    return Book.fromJson(json as Map<String, dynamic>);
  }

  Future<void> deleteBook(int id) => _api.delete('books/$id');
}
