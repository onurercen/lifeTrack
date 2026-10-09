/// One page of a list endpoint (`{items, page, size, totalItems, hasNext}`).
class PageResult<T> {
  const PageResult({required this.items, required this.hasNext});

  /// Records per page requested by the app.
  static const int pageSize = 20;

  final List<T> items;
  final bool hasNext;

  factory PageResult.fromJson(Map<String, dynamic> json, T Function(Map<String, dynamic> item) fromItem) {
    return PageResult(
      items: (json['items'] as List<dynamic>).map((e) => fromItem(e as Map<String, dynamic>)).toList(),
      hasNext: json['hasNext'] as bool,
    );
  }

  /// Query parameters for page [page] (zero based).
  static Map<String, String> query(int page) => {'page': '$page', 'size': '$pageSize'};
}
