import '../../../core/network/api_client.dart';
import '../../../core/network/page_result.dart';
import '../models/book.dart';

class BookRepository {
  const BookRepository(this._api);

  final ApiClient _api;

  Future<PageResult<Book>> fetchBooks({String? query, BookStatus? status, int page = 0}) async {
    final params = {
      ...PageResult.query(page),
      if (query != null) 'query': query,
      if (status != null) 'status': status.apiValue,
    };
    final json = await _api.get('books', query: params);
    return PageResult.fromJson(json as Map<String, dynamic>, Book.fromJson);
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
