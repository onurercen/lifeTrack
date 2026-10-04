import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:lifetrack_mobile/app/app_scope.dart';
import 'package:lifetrack_mobile/core/network/api_client.dart';
import 'package:lifetrack_mobile/features/books/presentation/books_screen.dart';

import '../../test_helpers.dart';

Map<String, dynamic> _bookJson(int id, String title, String author) => {
      'id': id,
      'title': title,
      'author': author,
      'description': 'Açıklama',
      'userEmail': 'ayse@test.com',
      'createdAt': '2026-10-04T07:30:00',
    };

Future<void> _pump(WidgetTester tester, http.Client client) async {
  final deps = AppDependencies.create(api: ApiClient(baseUrl: 'http://test/api', client: client));
  await tester.pumpWidget(AppScope(dependencies: deps, child: const MaterialApp(home: BooksScreen())));
  await tester.pumpAndSettle();
}

void main() {
  setUp(() => useInMemorySecureStorage());

  testWidgets('kitapları listeler ve arama sorgusunu backende iletir', (tester) async {
    final queries = <String?>[];
    await _pump(
      tester,
      mockBackend({
        'GET /api/books': (request) {
          final query = request.url.queryParameters['query'];
          queries.add(query);
          final all = [_bookJson(1, 'Dune', 'Frank Herbert'), _bookJson(2, 'Clean Code', 'Robert C. Martin')];
          return jsonResponse(query == null ? all : all.where((b) => (b['author'] as String).contains(query)).toList());
        },
      }),
    );

    expect(find.text('Dune'), findsOneWidget);
    expect(find.text('Clean Code'), findsOneWidget);

    await tester.enterText(find.byType(SearchBar), 'Herbert');
    await tester.pump(const Duration(milliseconds: 350));
    await tester.pumpAndSettle();

    expect(queries.last, 'Herbert');
    expect(find.text('Dune'), findsOneWidget);
    expect(find.text('Clean Code'), findsNothing);
  });

  testWidgets('arama sonuç vermezse bilgi mesajı gösterir', (tester) async {
    await _pump(
      tester,
      mockBackend({
        'GET /api/books': (request) =>
            jsonResponse(request.url.queryParameters.containsKey('query') ? [] : [_bookJson(1, 'Dune', 'F')]),
      }),
    );

    await tester.enterText(find.byType(SearchBar), 'yok');
    await tester.pump(const Duration(milliseconds: 350));
    await tester.pumpAndSettle();

    expect(find.text('Aramanla eşleşen kayıt yok.'), findsOneWidget);
  });

  testWidgets('kitabı düzenler', (tester) async {
    var books = [_bookJson(3, 'Dune', 'Frank Herbert')];
    Map<String, dynamic>? sent;
    await _pump(
      tester,
      mockBackend({
        'GET /api/books': (_) => jsonResponse(books),
        'PUT /api/books/3': (request) {
          sent = jsonDecode(request.body) as Map<String, dynamic>;
          books = [_bookJson(3, sent!['title'] as String, 'Frank Herbert')];
          return jsonResponse(books.first);
        },
      }),
    );

    await tester.tap(find.text('Dune'));
    await tester.pumpAndSettle();
    expect(find.text('Kitabı düzenle'), findsOneWidget);

    await tester.enterText(find.widgetWithText(TextFormField, 'Başlık'), 'Dune Messiah');
    await tester.tap(find.text('Kaydet'));
    await tester.pumpAndSettle();

    expect(sent, {'title': 'Dune Messiah', 'author': 'Frank Herbert', 'description': 'Açıklama'});
    expect(find.text('Dune Messiah'), findsOneWidget);
  });

  testWidgets('sunucu doğrulama hatasını ilgili alanda gösterir', (tester) async {
    await _pump(
      tester,
      mockBackend({
        'GET /api/books': (_) => jsonResponse([]),
        'POST /api/books': (_) => jsonResponse({
              'message': 'Gönderilen bilgiler geçersiz',
              'errors': {'author': 'Yazar adı çok uzun'},
            }, 400),
      }),
    );

    await tester.tap(find.text('Kitap ekle'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextFormField, 'Başlık'), 'X');
    await tester.enterText(find.widgetWithText(TextFormField, 'Yazar'), 'Y');
    await tester.enterText(find.widgetWithText(TextFormField, 'Açıklama (opsiyonel)'), 'Uzun açıklama');
    await tester.tap(find.text('Ekle'));
    await tester.pumpAndSettle();

    expect(find.text('Yazar adı çok uzun'), findsOneWidget);
    expect(find.text('Yeni kitap'), findsOneWidget); // form stays open
  });

  testWidgets('açıklamasız kitap eklenebilir', (tester) async {
    final books = <Map<String, dynamic>>[];
    Map<String, dynamic>? sent;
    await _pump(
      tester,
      mockBackend({
        'GET /api/books': (_) => jsonResponse(books),
        'POST /api/books': (request) {
          sent = jsonDecode(request.body) as Map<String, dynamic>;
          books.add({..._bookJson(1, 'Dune', 'Frank Herbert'), 'description': null});
          return jsonResponse(books.first, 201);
        },
      }),
    );

    await tester.tap(find.text('Kitap ekle'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextFormField, 'Başlık'), 'Dune');
    await tester.enterText(find.widgetWithText(TextFormField, 'Yazar'), 'Frank Herbert');
    await tester.tap(find.text('Ekle'));
    await tester.pumpAndSettle();

    expect(sent, {'title': 'Dune', 'author': 'Frank Herbert', 'description': null});
    expect(find.text('Dune'), findsOneWidget);
  });
}
